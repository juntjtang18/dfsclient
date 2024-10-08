package com.fdu.msacs.dfsclient.dfsclient;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class ByteArrayHttpMessageConverter implements HttpMessageConverter<byte[]> {
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return byte[].class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return byte[].class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public byte[] read(Class<? extends byte[]> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return StreamUtils.copyToByteArray(inputMessage.getBody());
    }

    @Override
    public void write(byte[] bytes, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        OutputStream out = outputMessage.getBody();
        out.write(bytes);
        out.flush();
    }
}
