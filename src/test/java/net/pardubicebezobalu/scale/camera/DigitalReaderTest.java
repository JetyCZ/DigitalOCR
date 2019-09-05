package net.pardubicebezobalu.scale.camera;

import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import static org.junit.jupiter.api.Assertions.*;

class DigitalReaderTest {

    Rect cropRect = new Rect(642, 610, 376, 184);
    static {
        VideoCap.initOpenCV();
    }

    @Test
    public void test() {
        Mat img  = Imgcodecs.imread(getClass().getResource("/IMG_20190328_183439.jpg").getFile());


        assertEquals(0, new DigitalReader().readDigits(img, cropRect));
    }

    @Test
    public void test2() {
        Mat img  = Imgcodecs.imread(getClass().getResource("/IMG_20190328_183450.jpg").getFile());


        assertEquals(234, new DigitalReader().readDigits(img, cropRect));
    }
    @Test
    public void test3() {
        Mat img  = Imgcodecs.imread(getClass().getResource("/IMG_20190328_183500.jpg").getFile());


        assertEquals(850, new DigitalReader().readDigits(img, cropRect));
    }
    @Test
    public void test4() {
        Mat img  = Imgcodecs.imread(getClass().getResource("/IMG_20190328_183514.jpg").getFile());


        assertEquals(1194, new DigitalReader().readDigits(img, cropRect));
    }
    @Test
    public void test5() {
        Mat img  = Imgcodecs.imread(getClass().getResource("/IMG_20190328_183522.jpg").getFile());
        assertEquals(1216, new DigitalReader().readDigits(img, cropRect));
    }

    @Test
    public void testSingleDigit4() {
        Mat img  = Imgcodecs.imread(getClass().getResource("/digits/4.png").getFile(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        assertEquals(4, new DigitalReader().analyzeOneDigit(img));
    }
}