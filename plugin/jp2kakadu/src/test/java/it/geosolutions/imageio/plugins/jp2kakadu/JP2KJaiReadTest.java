/*
 *    JImageIO-extension - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *	  https://imageio-ext.dev.java.net/
 *    (C) 2007, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.imageio.plugins.jp2kakadu;

import it.geosolutions.imageio.gdalframework.Viewer;
import it.geosolutions.imageio.plugins.jp2kakadu.JP2GDALKakaduImageReaderSpi.KakaduErrorManagement;
import it.geosolutions.resources.TestData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageReadParam;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Daniele Romagnoli, GeoSolutions.
 * @author Simone Giannecchini, GeoSolutions. 
 */
public class JP2KJaiReadTest extends AbstractJP2KTestCase {

	public final static boolean INTERMEDIATE_VISUALIZE = false;

	public final static String fileName = "sample.jp2";

	public JP2KJaiReadTest(String name) {
		super(name);
	}

	/**
	 * Simple test read
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void testRead() throws FileNotFoundException, IOException {
		final ParameterBlockJAI pbjImageRead;
		final File file = TestData.file(this, fileName);
		JP2GDALKakaduImageReaderSpi
				.setKakaduInputErrorManagement(KakaduErrorManagement.FAST);
		JP2GDALKakaduImageReaderSpi.setReadMultithreadingLevel(15);
		pbjImageRead = new ParameterBlockJAI("ImageRead");
		pbjImageRead.setParameter("Input", file);

		RenderedOp image = JAI.create("ImageRead", pbjImageRead);
		Viewer.visualize(image);
	}

	/**
	 * Test read exploiting common JAI operations (Crop-Translate-Rotate)
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void testJaiOperations() throws IOException {
		final File inputFile = TestData.file(this, fileName);

		JP2GDALKakaduImageReaderSpi
				.setKakaduInputErrorManagement(KakaduErrorManagement.FAST);
		JP2GDALKakaduImageReaderSpi.setReadMultithreadingLevel(5);

		// ////////////////////////////////////////////////////////////////
		// preparing to read
		// ////////////////////////////////////////////////////////////////
		final ParameterBlockJAI pbjImageRead;
		final ImageReadParam irp = new ImageReadParam();

		Integer xSubSampling = new Integer(1);
		Integer ySubSampling = new Integer(1);
		Integer xSubSamplingOffset = new Integer(0);
		Integer ySubSamplingOffset = new Integer(0);

		irp.setSourceSubsampling(xSubSampling.intValue(), ySubSampling
				.intValue(), xSubSamplingOffset.intValue(), ySubSamplingOffset
				.intValue());

		pbjImageRead = new ParameterBlockJAI("ImageRead");
		pbjImageRead.setParameter("Input", inputFile);
		pbjImageRead.setParameter("readParam", irp);
		RenderedOp image = JAI.create("ImageRead", pbjImageRead);

		if (INTERMEDIATE_VISUALIZE)
			Viewer.visualize(image, "subsampled");

		// ////////////////////////////////////////////////////////////////
		// preparing to crop
		// ////////////////////////////////////////////////////////////////
		final ParameterBlockJAI pbjCrop = new ParameterBlockJAI("Crop");
		pbjCrop.addSource(image);

		// Setting a square crop to avoid blanks zone when rotating.
		Float xCrop = new Float(image.getMinX() + image.getWidth() / 4);
		Float yCrop = new Float(image.getMinX() + image.getWidth() / 4);
		Float cropWidth = new Float(image.getWidth() / 4);
		Float cropHeigth = new Float(image.getWidth() / 4);
		pbjCrop.setParameter("x", xCrop);
		pbjCrop.setParameter("y", yCrop);
		pbjCrop.setParameter("width", cropWidth);
		pbjCrop.setParameter("height", cropHeigth);
		final RenderedOp croppedImage = JAI.create("Crop", pbjCrop);

		if (INTERMEDIATE_VISUALIZE)
			Viewer.visualize(croppedImage, "cropped");

		// ////////////////////////////////////////////////////////////////
		// preparing to translate
		// ////////////////////////////////////////////////////////////////
		final ParameterBlockJAI pbjTranslate = new ParameterBlockJAI(
				"Translate");
		pbjTranslate.addSource(croppedImage);
		Float xTrans = new Float(xCrop.floatValue() * (-1));
		Float yTrans = new Float(yCrop.floatValue() * (-1));
		pbjTranslate.setParameter("xTrans", xTrans);
		pbjTranslate.setParameter("yTrans", yTrans);
		final RenderedOp translatedImage = JAI
				.create("Translate", pbjTranslate);

		if (INTERMEDIATE_VISUALIZE)
			Viewer.visualize(translatedImage, "translated");

		// ////////////////////////////////////////////////////////////////
		// preparing to rotate
		// ////////////////////////////////////////////////////////////////
		final ParameterBlockJAI pbjRotate = new ParameterBlockJAI("Rotate");
		pbjRotate.addSource(translatedImage);

		Float xOrigin = new Float(cropWidth.floatValue() / 2);
		Float yOrigin = new Float(cropHeigth.floatValue() / 2);
		Float angle = new Float(java.lang.Math.PI / 2);

		pbjRotate.setParameter("xOrigin", xOrigin);
		pbjRotate.setParameter("yOrigin", yOrigin);
		pbjRotate.setParameter("angle", angle);

		final RenderedOp rotatedImage = JAI.create("Rotate", pbjRotate);

		StringBuffer title = new StringBuffer("SUBSAMP:").append("X[").append(
				xSubSampling.toString()).append("]-Y[").append(
				ySubSampling.toString()).append("]-Xof[").append(
				xSubSamplingOffset.toString()).append("]-Yof[").append(
				ySubSamplingOffset).append("]CROP:X[").append(xCrop.toString())
				.append("]-Y[").append(yCrop.toString()).append("]-W[").append(
						cropWidth.toString()).append("]-H[").append(
						cropHeigth.toString()).append("]TRANS:X[").append(
						xTrans.toString()).append("]-Y[").append(
						yTrans.toString()).append("]ROTATE:xOrig[").append(
						xOrigin.toString()).append("]-yOrig[").append(
						yOrigin.toString()).append("]-ang[").append(
						angle.toString()).append("]");

		Viewer.visualize(rotatedImage, title.toString());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Test read exploiting common JAI operations (Crop-Translate-Rotate)
		suite.addTest(new JP2KJaiReadTest("testJaiOperations"));

		// Test reading of a simple image
		suite.addTest(new JP2KJaiReadTest("testRead"));

		return suite;
	}

	public static void main(java.lang.String[] args) {
		junit.textui.TestRunner.run(suite());
	}

}
