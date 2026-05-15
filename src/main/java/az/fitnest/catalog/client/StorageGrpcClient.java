package az.fitnest.catalog.client;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.StorageFileData;
import az.fitnest.storage.grpc.DeleteFilesRequest;
import az.fitnest.storage.grpc.DeleteFilesResponse;
import az.fitnest.storage.grpc.DownloadFileRequest;
import az.fitnest.storage.grpc.DownloadFileResponse;
import az.fitnest.storage.grpc.FileMetadata;
import az.fitnest.storage.grpc.GetDownloadUrlRequest;
import az.fitnest.storage.grpc.GetDownloadUrlResponse;
import az.fitnest.storage.grpc.StorageServiceGrpc;
import az.fitnest.storage.grpc.UploadFileRequest;
import az.fitnest.storage.grpc.UploadFileResponse;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageGrpcClient {
    @GrpcClient(value = "storage-backend")
    private StorageServiceGrpc.StorageServiceStub asyncStub;
    @GrpcClient(value = "storage-backend")
    private StorageServiceGrpc.StorageServiceBlockingStub blockingStub;

    @Value("${grpc.storage.unary.deadline.seconds:30}")
    private long unaryDeadlineSeconds;

    @Value("${grpc.storage.stream.deadline.seconds:300}")
    private long streamDeadlineSeconds;

    public StorageFileData uploadFile(MultipartFile file, String directory) {
        return this.uploadFile(file, directory, null);
    }

    public StorageFileData uploadFile(MultipartFile file, String directory, String oldPath) {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicReference<StorageFileData> responseData = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        StreamObserver<UploadFileResponse> responseObserver = new StreamObserver<UploadFileResponse>() {
            @Override
            public void onNext(UploadFileResponse response) {
                if (response.getSuccess()) {
                    az.fitnest.storage.grpc.StorageFileData grpcData = response.getData();
                    StorageFileData data = new StorageFileData(
                        grpcData.getPath(),
                        grpcData.getSize(),
                        grpcData.getMd5(),
                        grpcData.getFsId()
                    );
                    responseData.set(data);
                } else {
                }
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };

        StreamObserver<UploadFileRequest> requestObserver = this.asyncStub.uploadFile(responseObserver);
        try {
            FileMetadata.Builder metadataBuilder = FileMetadata.newBuilder()
                .setFilename(file.getOriginalFilename())
                .setDirectory(directory != null ? directory : "/uploads")
                .setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");

            if (oldPath != null) {
                metadataBuilder.setOldPath(oldPath);
            }

            requestObserver.onNext(UploadFileRequest.newBuilder().setMetadata(metadataBuilder.build()).build());

            byte[] buffer = new byte[65536];
            try (InputStream inputStream = file.getInputStream()) {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    requestObserver.onNext(UploadFileRequest.newBuilder().setChunkData(ByteString.copyFrom(buffer, 0, bytesRead)).build());
                }
            }
            requestObserver.onCompleted();

            if (!finishLatch.await(5L, TimeUnit.MINUTES)) {
                throw new RuntimeException("error.rpc_failed");
            }
            if (error.get() != null) {
                throw new RuntimeException("error.file_upload_failed", error.get());
            }

            StorageFileData finalData = responseData.get();
            if (finalData == null) {
                throw new RuntimeException("error.file_upload_failed");
            }
            return finalData;
        } catch (IOException | InterruptedException e) {
            requestObserver.onError(e);
            throw new RuntimeException("error.file_upload_failed", e);
        }
    }

    public String getDownloadUrl(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) return null;
        return "/api/v1/media/stream/" + fileId;
    }

    public void deleteFiles(List<String> paths) {
        DeleteFilesRequest request = DeleteFilesRequest.newBuilder().addAllPaths(paths).build();
        try {
            DeleteFilesResponse response = this.blockingStub
                    .withDeadlineAfter(unaryDeadlineSeconds, TimeUnit.SECONDS)
                    .deleteFiles(request);
            if (!response.getSuccess()) {
                throw new RuntimeException("error.rpc_failed: " + response.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("error.rpc_failed", e);
        }
    }

    public void downloadFile(String fileId, Consumer<DownloadFileResponse> observer) {
        DownloadFileRequest request = DownloadFileRequest.newBuilder().setFileId(fileId).build();
        try {
            this.blockingStub
                    .withDeadlineAfter(streamDeadlineSeconds, TimeUnit.SECONDS)
                    .downloadFile(request)
                    .forEachRemaining(response -> {
                        observer.accept(response);
                    });
        } catch (Exception e) {
            throw new RuntimeException("error.rpc_failed", e);
        }
    }
}
