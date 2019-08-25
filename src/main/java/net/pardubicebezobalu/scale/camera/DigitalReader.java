package net.pardubicebezobalu.scale.camera;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;

public class DigitalReader {

    Map<String, Integer> digitMap = new HashMap<>();
    {
        digitMap.put("1111101", 0);
        digitMap.put("0110111", 2);
        digitMap.put("0101111", 3);
        digitMap.put("1101010", 4);
        digitMap.put("1001111", 5);
        digitMap.put("1011111", 6);
        digitMap.put("0101100", 7);
        digitMap.put("1111111", 8);
        digitMap.put("1101111", 9);
    }

    public int readDigits(Mat img1, Rect cropRect) {
        if (true) return 255;
        Mat gray = getCroppedGray(img1, cropRect);

        int width = gray.cols();
        int height = gray.rows();

        List<Mat> kmeans = kmeans(width, height, gray);
        gray.release();

        Mat digits = kmeans.get(1);
        Imgproc.dilate(digits, digits, Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(1,7)));

        List<MatOfPoint> contours = findContours(digits);


        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                Rect rect1 = Imgproc.boundingRect(o1);
                Rect rect2 = Imgproc.boundingRect(o2);
                int result = Double.compare(rect1.tl().y, rect2.tl().y);
                return result;
            }
        } );
        drawContour(digits, contours.get(contours.size()-1), Color.BLACK, -1);
        contours.remove(contours.size()-1);
        List goodContours = new ArrayList();
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.width<digits.width()/2 && rect.height>digits.height()/3) {
                goodContours.add(contour);
            } else {
                drawContour(digits, contour, Color.BLACK, -1);
            }
        }

        if (goodContours.size()==0) {
            throw new IllegalStateException("No light digits found in image");
        }
        digits = cropUsingContours(digits, goodContours);

        List<MatOfPoint> fourDigits = findContours(digits);
        if (fourDigits.size()!=4) {
            throw new IllegalStateException("Expecting 4 contours but got " + fourDigits.size());
        }
        Collections.sort(fourDigits, (o1, o2) -> {
            Rect rect1 = Imgproc.boundingRect(o1);
            Rect rect2 = Imgproc.boundingRect(o2);
            int result = Double.compare(rect1.tl().x, rect2.tl().x);
            return result;
        });



        StringBuffer digitsStr = new StringBuffer();

        for (MatOfPoint fourDigit : fourDigits) {
            StringBuffer segments = new StringBuffer();
            Rect digitContour = Imgproc.boundingRect(fourDigit);

            int w = digitContour.width;
            int h = digitContour.height;

            if ((double) h/w >2.8) {
                digitsStr.append("1");
            } else {
                //digitContour = new Rect(digitContour.x, digitContour.y, digitContour.width-5, digitContour.height-5);
                Mat oneDigit = digits.submat(digitContour);

                double quaterHeight = (double) h /4;
                double halfWidth = (double) w /2;
                double thirdHeight = (double) h /3;
                Mat topRowLeft = oneDigit.submat(new Rect(new Point(0, (int) quaterHeight), new Point((int) halfWidth, (int) quaterHeight+1)));
                Mat topRowRight = oneDigit.submat(new Rect(new Point(halfWidth, (int) quaterHeight), new Point((int) w, (int) quaterHeight+1)));

                Mat bottomRowLeft = oneDigit.submat(new Rect(new Point(0, (int) 3*quaterHeight), new Point((int) halfWidth, (int) 3*quaterHeight+1)));
                Mat bottomRowRight = oneDigit.submat(new Rect(new Point(halfWidth, (int) 3*quaterHeight), new Point((int) w, (int) 3*quaterHeight+1)));

                Mat middleColumnTop = oneDigit.submat(new Rect(new Point(halfWidth, 0), new Point(halfWidth+1, thirdHeight)));
                Mat middleColumnMiddle = oneDigit.submat(new Rect(new Point(halfWidth, thirdHeight), new Point(halfWidth+1, 2*thirdHeight)));
                Mat middleColumnBottom = oneDigit.submat(new Rect(new Point(halfWidth, 2*thirdHeight), new Point(halfWidth+1, h)));

                List<Mat> analyzeSegments = new ArrayList<>();
                analyzeSegments.add(topRowLeft);
                analyzeSegments.add(topRowRight);
                analyzeSegments.add(bottomRowLeft);
                analyzeSegments.add(bottomRowRight);
                analyzeSegments.add(middleColumnTop);
                analyzeSegments.add(middleColumnMiddle);
                analyzeSegments.add(middleColumnBottom);

                for (Mat analyzeSegment : analyzeSegments) {
                    segments.append(findContours(analyzeSegment).size());
                }
                if (!digitMap.containsKey(segments.toString())) {
                    throw new IllegalStateException("Unmappable digit: " + segments.toString());
                }
                digitsStr.append(digitMap.get(segments.toString()));
            }

        }

        Integer integer = Integer.valueOf(digitsStr.toString());

        for (Mat kmean : kmeans) {
            kmean.release();
        }


        return integer;
    }

    private Mat getCroppedGray(Mat img1, Rect cropRect) {
        Mat cropped = img1.submat(cropRect);

        Mat gray = new Mat();
        Imgproc.cvtColor(cropped, gray, Imgproc.COLOR_BGR2GRAY);
        cropped.release();
        return gray;
    }

    private List<Mat> kmeans(int width, int height, Mat gray) {
        Mat samples32f = new Mat();

        Mat allPixelsInOneRow = gray.reshape(1, width * height);
        allPixelsInOneRow.convertTo(samples32f, CvType.CV_32F, 1.0 / 255.0);
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT, 100, 1);
        Mat labels = new Mat(width*height, 1, CvType.CV_32SC1);
        labels.setTo(new Scalar(0));
        Mat centers = new Mat();
        Core.kmeans(samples32f, 2,
                labels,
                criteria,
                1,
                Core.KMEANS_PP_CENTERS + Core.KMEANS_USE_INITIAL_LABELS, centers);

        samples32f.release();
        allPixelsInOneRow.release();

        List<Mat> mats = showClusters(gray, labels, centers, 2);
        labels.release();
        centers.release();
        return mats;
    }

    private Mat cropUsingContours(Mat imgToCrop, List<MatOfPoint> contours) {
        int minX = Integer.MAX_VALUE, maxX = 0;
        int minY = Integer.MAX_VALUE, maxY = 0;

        Mat rects = imgToCrop.clone();

        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            draw(rects, rect, new Scalar(255), 1);
            minX = Math.min(minX, rect.x);
            maxX = Math.max(maxX, rect.x+rect.width-1);
            minY = Math.min(minY, rect.y);
            maxY = Math.max(maxY, rect.y+rect.height-1);
        }
        Rect cropRect = new Rect(new Point(minX, minY), new Point(maxX, maxY));
        return imgToCrop.submat(cropRect);
    }

    private List<MatOfPoint> findContours(Mat img) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(img, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        return contours;
    }


    public static void draw(Mat img, Rect rect, Scalar color, int thickness) {
        if (rect==null) {
            return;
        }
        Point topLeft = new Point(rect.x, rect.y);
        int right = rect.x + rect.width;
        Point topRight = new Point(right, rect.y);
        int bottom = rect.y + rect.height;
        Point bottomLeft = new Point(rect.x, bottom);
        Point bottomRight = new Point(right, bottom);

        drawLine(img, topLeft, topRight, color, thickness);
        drawLine(img, topRight, bottomRight, color, thickness);
        drawLine(img, bottomRight, bottomLeft, color, thickness);
        drawLine(img, bottomLeft, topLeft, color, thickness);
    }
    public static void drawLine(Mat bill, Point from, Point to, Scalar color, int thickness) {
        Imgproc.line(bill, from, to, color, thickness);
    }

    int mostBlackCenter;
    private List<Mat> showClusters(Mat cutout, Mat labels, Mat centers, int k) {

        mostBlackCenter = 0;

        centers.convertTo(centers, CvType.CV_8UC1, 255.0);
        centers.reshape(k);

        List<Mat> clusters = new ArrayList<Mat>();
        double mostBlackCenterValue = 255;
        for(int i = 0; i < centers.rows(); i++) {
            double currentCenterValue = centers.get(i, 0)[0];
            if (currentCenterValue <mostBlackCenterValue) {
                mostBlackCenter = i;
                mostBlackCenterValue = currentCenterValue;
            }
            clusters.add(Mat.zeros(cutout.size(), CvType.CV_8U));
        }

        Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        for(int i = 0; i < centers.rows(); i++) counts.put(i, 0);

        int rows = 0;
        for(int y = 0; y < cutout.rows(); y++) {
            for(int x = 0; x < cutout.cols(); x++) {
                int label = (int)labels.get(rows, 0)[0];
                counts.put(label, counts.get(label) + 1);
                clusters.get(label).put(y, x, 255);
                rows++;
            }
        }
        return clusters;
    }


    public static void f(Mat img) {
        try {
            String tempImgFile = "/tmp/a" + UUID.randomUUID().toString().substring(0,4) + ".png";
            Imgcodecs.imwrite(tempImgFile, img);
            Runtime.getRuntime().exec("firefox "
                    + tempImgFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void drawContour(Mat img, MatOfPoint contour, Color color, int thickness) {
        Imgproc.drawContours(img, Collections.singletonList(contour), 0, new Scalar(color.getBlue(), color.getGreen(), color.getRed()),thickness);
    }

    public static void drawContour(Mat img, MatOfPoint contour, Color color) {
        drawContour(img, contour, color, 2);
    }
    public static void drawContourInRect(Mat img, MatOfPoint contour, Color color, int thickness, Rect rect) {
        Imgproc.drawContours(img, Collections.singletonList(contour), 0, new Scalar(color.getBlue(), color.getGreen(), color.getRed()), thickness
                // int lineType, Mat hierarchy, int maxLevel, Point offset
                , 8, new Mat(), 10, new Point(rect.x, rect.y)
        );
    }


}
