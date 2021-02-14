package com.radishframework.grpc.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Charsets;

import io.kubernetes.client.proto.V1.EndpointSubset;
import io.kubernetes.client.proto.V1.Endpoints;
import io.kubernetes.client.proto.V1.EndpointsList;
import io.kubernetes.client.proto.V1.Event;

public class KubernetesClient {

    private final String kubeApiUri;

    public KubernetesClient(String kubeApiUri) {
        this.kubeApiUri = kubeApiUri;
    }

    public List<EndpointSubset> getEndpointSubsets(String namespace, String name)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.kubeApiUri + "/api/v1/namespaces/" + namespace + "/endpoints?filedSelector="
                        + URLEncoder.encode("name=" + name, Charsets.UTF_8)))
                .header("Accept", "application/vnd.kubernetes.protobuf").version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(20)).GET().build();
        HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());

        List<EndpointSubset> subsets = Collections.emptyList();

        if (response.statusCode() == 200) {
            EndpointsList endpointsList = EndpointsList.parseFrom(response.body());
            if (endpointsList.getItemsCount() > 0) {
                Endpoints endpoints = endpointsList.getItems(0);
                if (endpoints.getSubsetsCount() > 0) {
                    subsets = endpoints.getSubsetsList();
                }
            }
        }

        return subsets;
    }

    public void watchEndpointSubsets(String namespace, String name) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.kubeApiUri + "/api/v1/namespaces/" + namespace
                        + "/endpoints?watch=true&filedSelector=" + URLEncoder.encode("name=" + name, Charsets.UTF_8)))
                .header("Accept", "application/vnd.kubernetes.protobuf").version(HttpClient.Version.HTTP_1_1).GET()
                .timeout(Duration.ofSeconds(20)).build();
        client.sendAsync(request, BodyHandlers.ofInputStream()).thenAccept(response -> {
            ChunkedInputStream in = new ChunkedInputStream(response.body());
            int size = in.readChunkSize();
        });
    }

    private class ChunkedInputStream {
        /** The inputstream that we're wrapping */
        private InputStream in;

        /** The chunk size */
        private int chunkSize;

        /** The current position within the current chunk */
        private int pos;

        /** True if we'are at the beginning of stream */
        private boolean bof = true;

        /** True if we've reached the end of stream */
        private boolean eof = false;

        /** True if this stream is closed */
        private boolean closed = false;

        public ChunkedInputStream(InputStream in) {
            this.in = in;
        }

        public int readChunk(byte[] buffer, int size) {
			return in.read(buffer, 0, size);
		}

		/**
         * Read the CRLF terminator.
         * 
         * @throws IOException If an IO error occurs.
         */
        private void readCRLF() throws IOException {
            int cr = in.read();
            int lf = in.read();
            if ((cr != '\r') || (lf != '\n')) {
                throw new IOException("CRLF expected at end of chunk: " + cr + "/" + lf);
            }
        }

        /**
         * Read the next chunk.
         * 
         * @throws IOException If an IO error occurs.
         */
        public int readChunkSize() throws IOException {
            if (!bof) {
                readCRLF();
            }
            chunkSize = getChunkSizeFromInputStream(in);
            bof = false;
            pos = 0;
            if (chunkSize == 0) {
                eof = true;
                readCRLF();
            }

            return chunkSize;
        }

        /**
         * Expects the stream to start with a chunksize in hex with optional comments
         * after a semicolon. The line must end with a CRLF: "a3; some comment\r\n"
         * Positions the stream at the start of the next line.
         *
         * @param in The new input stream.
         * 
         * @return the chunk size as integer
         * 
         * @throws IOException when the chunk size could not be parsed
         */
        private int getChunkSizeFromInputStream(final InputStream in) throws IOException {
    
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // States: 0=normal, 1=\r was scanned, 2=inside quoted string, -1=end
            int state = 0;
            while (state != -1) {
                int b = in.read();
                if (b == -1) {
                    throw new IOException("chunked stream ended unexpectedly");
                }
                switch (state) {
                    case 0:
                        switch (b) {
                            case '\r':
                                state = 1;
                                break;
                            case '\"':
                                state = 2;
                                /* fall through */
                            default:
                                baos.write(b);
                        }
                        break;
    
                    case 1:
                        if (b == '\n') {
                            state = -1;
                        } else {
                            // this was not CRLF
                            throw new IOException(
                                    "Protocol violation: Unexpected" + " single newline character in chunk size");
                        }
                        break;
    
                    case 2:
                        switch (b) {
                            case '\\':
                                b = in.read();
                                baos.write(b);
                                break;
                            case '\"':
                                state = 0;
                                /* fall through */
                            default:
                                baos.write(b);
                        }
                        break;
                    default:
                        throw new RuntimeException("assertion failed");
                }
            }
    
            String dataString = new String(baos.toByteArray(), Charsets.US_ASCII);
            int result;
            try {
                result = Integer.parseInt(dataString.trim(), 16);
            } catch (NumberFormatException e) {
                throw new IOException("Bad chunk size: " + dataString);
            }
            return result;
        }
    }
}
