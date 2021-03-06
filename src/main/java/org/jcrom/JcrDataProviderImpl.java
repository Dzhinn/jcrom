/**
 * This file is part of the JCROM project.
 * Copyright (C) 2008-2015 - All rights reserved.
 * Authors: Olafur Gauti Gudmundsson, Nicolas Dos Santos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jcrom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;

/**
 * A simple implementation of the JcrDataProvider interface.
 * Developers can implement their own data provider if advanced or custom
 * functionality is needed.
 * 
 * <p>Thanks to Robin Wyles for adding content length.</p>
 * 
 * @author Olafur Gauti Gudmundsson
 * @author Nicolas Dos Santos
 */
public class JcrDataProviderImpl implements JcrDataProvider {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(JcrDataProviderImpl.class.getName());

    private final TYPE type;
    private final byte[] bytes;
    private final File file;
    private final InputStream inputStream;
    private final Binary binary;
    private final long contentLength;

    public JcrDataProviderImpl(byte[] bytes) {
        this.type = TYPE.BYTES;
        this.bytes = new byte[bytes.length];
        System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
        this.file = null;
        this.inputStream = null;
        this.binary = null;
        this.contentLength = bytes.length;
    }

    public JcrDataProviderImpl(File file) {
        this.type = TYPE.FILE;
        this.file = file;
        this.bytes = null;
        this.inputStream = null;
        this.binary = null;
        this.contentLength = file.length();
    }

    public JcrDataProviderImpl(InputStream inputStream) {
        this(inputStream, -1);
    }

    public JcrDataProviderImpl(InputStream inputStream, long length) {
        this.type = TYPE.STREAM;
        this.inputStream = inputStream;
        this.bytes = null;
        this.file = null;
        this.binary = null;
        this.contentLength = length;
    }

    /**
     * Constructor to directly maps the Binary of the JCR repository.
     * <p/>
     * This allow to get a fresh InputStream on each getInputstream call.
     * <p/>
     * This is also useful to know that this DataProvider should not be used to update a Node (cf. {@link #isPersisted()}).
     * @param binary Binary object
     */
    public JcrDataProviderImpl(Binary binary) {
        this.type = TYPE.STREAM;
        this.binary = binary;
        this.bytes = null;
        this.file = null;
        this.inputStream = null;
        this.contentLength = -1;
    }

    @Override
    public boolean isBytes() {
        return type == TYPE.BYTES;
    }

    @Override
    public boolean isFile() {
        return type == TYPE.FILE;
    }

    @Override
    public boolean isStream() {
        return type == TYPE.STREAM;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public InputStream getInputStream() {
        InputStream is = inputStream;
        try {
            is = binary != null ? binary.getStream() : inputStream;
        } catch (RepositoryException e) {
            logger.info(e.toString());
        }
        return is;
    }

    @Override
    public TYPE getType() {
        return type;
    }

    @Override
    public void writeToFile(File destination) throws IOException {
        if (getType() == TYPE.BYTES) {
            write(getBytes(), destination);
        } else if (getType() == TYPE.STREAM) {
            write(getInputStream(), destination);
        } else if (getType() == TYPE.FILE) {
            write(getFile(), destination);
        }
    }

    protected static void write(InputStream in, File destination) throws IOException {
        if (!destination.exists()) {
            destination.createNewFile();
        }

        OutputStream out = new FileOutputStream(destination);
        try {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    protected static void write(byte[] bytes, File destination) throws IOException {

        if (!destination.exists()) {
            destination.createNewFile();
        }

        FileOutputStream fileOutputStream = new FileOutputStream(destination);
        try {
            fileOutputStream.write(bytes);
        } finally {
            fileOutputStream.close();
        }
    }

    protected static void write(File source, File destination) throws IOException {

        FileInputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(destination);

        int doneCnt = -1, bufSize = 32768;
        byte buf[] = new byte[bufSize];

        try {
            while ((doneCnt = in.read(buf, 0, bufSize)) >= 0) {
                if (doneCnt == 0) {
                    Thread.yield();
                } else {
                    out.write(buf, 0, doneCnt);
                }
            }
            out.flush();
        } finally {
            in.close();
            out.close();
        }
    }

    @Override
    public long getContentLength() {
        long size = this.contentLength;
        try {
            size = binary != null ? binary.getSize() : this.contentLength;
        } catch (RepositoryException e) {
            logger.info(e.toString());
        }
        return size;
    }

    @Override
    public boolean isPersisted() {
        return binary != null;
    }

}
