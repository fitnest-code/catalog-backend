package az.fitnest.catalog.client;

import lombok.extern.slf4j.Slf4j;
import az.fitnest.catalog.dto.StorageFileData;
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
@Slf4j
public class StorageGrpcClient {
    @GrpcClient(value = "storage-service")
    private StorageServiceGrpc.StorageServiceStub asyncStub;
    @GrpcClient(value = "storage-service")
    private StorageServiceGrpc.StorageServiceBlockingStub blockingStub;

    // Deadline for short unary calls (seconds)
    @Value("${grpc.storage.unary.deadline.seconds:30}")
    private long unaryDeadlineSeconds;

    // Deadline for streaming downloads (seconds)
    @Value("${grpc.storage.stream.deadline.seconds:300}")
    private long streamDeadlineSeconds;

    public StorageFileData uploadFile(MultipartFile file, String directory) {
        return this.uploadFile(file, directory, null);
    }

    public StorageFileData uploadFile(MultipartFile file, String directory, String oldPath) {
        log.info("Starting gRPC uploadFile for file: {}, directory: {}", file.getOriginalFilename(), directory);
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicReference<StorageFileData> responseData = new AtomicReference<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        StreamObserver<UploadFileResponse> responseObserver = new StreamObserver<UploadFileResponse>() {
            @Override
            public void onNext(UploadFileResponse response) {
                log.info("gRPC responseObserver onNext received success={}", response.getSuccess());
                if (response.getSuccess()) {
                    az.fitnest.storage.grpc.StorageFileData grpcData = response.getData();
                    StorageFileData data = new StorageFileData();
                    data.setPath(grpcData.getPath());
                    data.setSize(grpcData.getSize());
                    data.setMd5(grpcData.getMd5());
                    data.setFsId(grpcData.getFsId());
                    responseData.set(data);
                } else {
                    log.error("gRPC upload response indicated failure: {}", response.getMessage());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC responseObserver onError triggered", t);
                error.set(t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("gRPC responseObserver onCompleted triggered");
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

            log.info("Sending gRPC metadata request");
            requestObserver.onNext(UploadFileRequest.newBuilder().setMetadata(metadataBuilder.build()).build());

            byte[] buffer = new byte[65536];
            log.info("Sending gRPC chunk data stream");
            try (InputStream inputStream = file.getInputStream()) {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    requestObserver.onNext(UploadFileRequest.newBuilder().setChunkData(ByteString.copyFrom(buffer, 0, bytesRead)).build());
                }
            }
            log.info("gRPC file stream fully sent, calling requestObserver.onCompleted()");
            requestObserver.onCompleted();

            if (!finishLatch.await(5L, TimeUnit.MINUTES)) {
                log.error("gRPC upload timed out after 5 minutes waiting for server response");
                throw new RuntimeException("error.rpc_failed");
            }
            if (error.get() != null) {
                log.error("gRPC upload encountered error after finishLatch await", error.get());
                throw new RuntimeException("error.file_upload_failed", error.get());
            }

            StorageFileData finalData = responseData.get();
            if (finalData == null) {
                log.error("gRPC responseData was null after successful completion");
                throw new RuntimeException("error.file_upload_failed");
            }
            log.info("gRPC upload completed successfully, returning final responseData fsId={}", finalData.getFsId());
            return finalData;
        } catch (IOException | InterruptedException e) {
            log.error("gRPC upload interrupted or threw IOException", e);
            requestObserver.onError(e);
            throw new RuntimeException("error.file_upload_failed", e);
        }
    }

    public String getDownloadUrl(String fileId) {
        GetDownloadUrlRequest request = GetDownloadUrlRequest.newBuilder().setFileId(fileId).build();
        try {
            GetDownloadUrlResponse response = this.blockingStub
                    .withDeadlineAfter(unaryDeadlineSeconds, TimeUnit.SECONDS)
                    .getDownloadUrl(request);
            if (response.getSuccess()) {
                return response.getDownloadUrl();
            }
            throw new RuntimeException("error.rpc_failed: " + response.getMessage());
        } catch (Exception e) {
            log.error("gRPC getDownloadUrl failed for fileId={}", fileId, e);
            throw new RuntimeException("error.rpc_failed", e);
        }
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
            log.error("gRPC deleteFiles failed for paths={} ", paths, e);
            throw new RuntimeException("error.rpc_failed", e);
        }
    }

    public void downloadFile(String fileId, Consumer<DownloadFileResponse> observer) {
        log.debug("[StorageGrpcClient] downloadFile called for fileId={}", fileId);
        DownloadFileRequest request = DownloadFileRequest.newBuilder().setFileId(fileId).build();
        try {
            this.blockingStub
                    .withDeadlineAfter(streamDeadlineSeconds, TimeUnit.SECONDS)
                    .downloadFile(request)
                    .forEachRemaining(response -> {
                        observer.accept(response);
                    });
            log.debug("[StorageGrpcClient] downloadFile completed for fileId={}", fileId);
        } catch (Exception e) {
            log.error("[StorageGrpcClient] downloadFile error for fileId={}", fileId, e);
            throw new RuntimeException("error.rpc_failed", e);
        }
    }
}
