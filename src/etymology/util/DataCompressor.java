/**
    Copyright (C) 2010-2012 University of Helsinki.    

    This file is part of Etymon.
    Etymon is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Etymon is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Etymon.  If not, see <http://www.gnu.org/licenses/>.
**/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package etymology.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

/**
 *
 * @author avihavai
 */
public class DataCompressor {
    public enum CompressionMethod {
        GZIP, BZIP2
    }

    private CompressionMethod method;
    public DataCompressor(final CompressionMethod method) {
        this.method = method;
    }

    public byte[] compress(String data) throws IOException {
        return compress(data.getBytes());
    }

    public byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream out = null;
        switch(method) {
            case GZIP:
                out = new GZIPOutputStream(baos);
                break;
            case BZIP2:
                out = new CBZip2OutputStream(baos);
                break;
        }
        
        out.write(data);
        out.close();
        return baos.toByteArray();
    }
}
