/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.ocr.filter.internal;

import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.ocr.api.OCRException;
import org.xwiki.contrib.ocr.api.TessBaseAPIProvider;

import static org.bytedeco.javacpp.lept.pixDestroy;
import static org.bytedeco.javacpp.lept.pixReadMem;

/**
 * Handle the different steps for parsing a file using the Tesseract OCR library.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = OCRParser.class)
@Singleton
public class OCRParser
{
    @Inject
    private TessBaseAPIProvider apiProvider;

    /**
     * Parse the given image (as a byte array) and return its contents.
     *
     * @param image the image to parse
     * @return the generated document
     * @throws OCRException if an error occurs during the importation
     */
    public OCRDocument parseImage(byte[] image) throws OCRException
    {
        lept.PIX leptImage = null;
        TessBaseAPI api = apiProvider.get();

        try {
            leptImage = pixReadMem(image, image.length);
            api.SetImage(leptImage);
        } finally {
            pixDestroy(leptImage);
        }

        return new OCRDocument(api);
    }

    /**
     * Parse the given image file and return its contents.
     *
     * @param image the image to parse
     * @return the generated document
     * @throws OCRException if an error occurs during the importation
     */
    public OCRDocument parseImage(Image image) throws OCRException
    {
        return parseImage(toByteArray(image));
    }

    /**
     * Converts a given {@link Image} to a byte array.
     *
     * @param image the image to convert
     * @return the associated byte array
     * @throws OCRException if the conversion failed
     */
    private byte[] toByteArray(Image image) throws OCRException
    {
        BufferedImage bufferedImage;
        if (image instanceof BufferedImage) {
            bufferedImage = (BufferedImage) image;
        } else {
            bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = bufferedImage.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
            byteArrayOutputStream.flush();
        } catch (IOException e) {
            throw new OCRException("Failed to convert the given image to byte array.", e);
        }

        return byteArrayOutputStream.toByteArray();
    }
}