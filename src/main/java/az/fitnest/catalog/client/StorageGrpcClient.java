/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.protobuf.ByteString
 *  io.grpc.stub.StreamObserver
 *  net.devh.boot.grpc.client.inject.GrpcClient
 *  org.springframework.stereotype.Service
 *  org.springframework.web.multipart.MultipartFile
 */
package az.fitnest.catalog.client;

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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageGrpcClient {
    @GrpcClient(value="storage-service")
    private StorageServiceGrpc.StorageServiceStub asyncStub;
    @GrpcClient(value="storage-service")
    private StorageServiceGrpc.StorageServiceBlockingStub blockingStub;

    public StorageFileData uploadFile(MultipartFile file, String directory) {
        return this.uploadFile(file, directory, null);
    }

    public StorageFileData uploadFile(MultipartFile file, String directory, String oldPath) {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicReference responseData = new AtomicReference();
        final AtomicReference error = new AtomicReference();
        StreamObserver<UploadFileResponse> responseObserver = new StreamObserver<UploadFileResponse>(){

            public void onNext(UploadFileResponse response) {
                if (response.getSuccess()) {
                    az.fitnest.storage.grpc.StorageFileData grpcData = response.getData();
                    StorageFileData data = new StorageFileData();
                    data.setPath(grpcData.getPath());
                    data.setSize(grpcData.getSize());
                    data.setMd5(grpcData.getMd5());
                    data.setFsId(grpcData.getFsId());
                    responseData.set(data);
                }
            }

            public void onError(Throwable t) {
                error.set(t);
                finishLatch.countDown();
            }

            public void onCompleted() {
                finishLatch.countDown();
            }
        };
        StreamObserver<UploadFileRequest> requestObserver = this.asyncStub.uploadFile(responseObserver);
        try {
            FileMetadata.Builder metadataBuilder = FileMetadata.newBuilder().setFilename(file.getOriginalFilename()).setDirectory(directory != null ? directory : "/uploads").setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
            if (oldPath != null) {
                metadataBuilder.setOldPath(oldPath);
            }
            requestObserver.onNext(UploadFileRequest.newBuilder().setMetadata(metadataBuilder.build()).build());
            byte[] buffer = new byte[65536];
            try (InputStream inputStream = file.getInputStream();){
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    requestObserver.onNext(UploadFileRequest.newBuilder().setChunkData(ByteString.copyFrom((byte[])buffer, (int)0, (int)bytesRead)).build());
                }
            }
            requestObserver.onCompleted();
            if (!finishLatch.await(5L, TimeUnit.MINUTES)) {
                throw new RuntimeException("Upload timed out");
            }
            if (error.get() != null) {
                throw new RuntimeException("Upload failed: " + ((Throwable)error.get()).getMessage(), (Throwable)error.get());
            }
            return (StorageFileData)responseData.get();
        }
        catch (IOException | InterruptedException e) {
            requestObserver.onError(e);
            throw new RuntimeException("Upload interrupted or failed", e);
        }
    }

    public String getDownloadUrl(String fileId) {
        GetDownloadUrlRequest request = GetDownloadUrlRequest.newBuilder().setFileId(fileId).build();
        GetDownloadUrlResponse response = this.blockingStub.getDownloadUrl(request);
        if (response.getSuccess()) {
            return response.getDownloadUrl();
        }
        throw new RuntimeException("Download failed: " + response.getMessage());
    }

    public void deleteFiles(List<String> paths) {
        DeleteFilesRequest request = DeleteFilesRequest.newBuilder().addAllPaths(paths).build();
        DeleteFilesResponse response = this.blockingStub.deleteFiles(request);
        if (!response.getSuccess()) {
            throw new RuntimeException("Delete failed: " + response.getMessage());
        }
    }

    public void downloadFile(String fileId, Consumer<DownloadFileResponse> observer) {
        DownloadFileRequest request = DownloadFileRequest.newBuilder().setFileId(fileId).build();
        this.blockingStub.downloadFile(request).forEachRemaining(observer);
    }
}

