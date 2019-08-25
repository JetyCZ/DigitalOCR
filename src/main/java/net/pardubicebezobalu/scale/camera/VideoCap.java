package net.pardubicebezobalu.scale.camera;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoCap {
    DigitalReader digitalReader = new DigitalReader();

    static {
        initOpenCV();
    }

    private MyFrame myFrame;

    public void setMyFrame(MyFrame myFrame) {
        this.myFrame = myFrame;
    }

    public static void initOpenCV() {
        System.load("/usr/share/OpenCV/java/libopencv_java340.so");
    }

    int lastSendToServer = -1;
    VideoCapture cap;
    Mat2Image mat2Img = new Mat2Image();

    VideoCap(){
        cap = new VideoCapture();
        cap.open(0);
    }

    BufferedImage getOneFrame() {
        Mat frameMat = mat2Img.mat;
        cap.read(frameMat);

        Rect cropRect = new Rect(MyFrame.x, MyFrame.y, MyFrame.width, MyFrame.height);
        try {
            int digits = digitalReader.readDigits(frameMat, cropRect);

            String fromServer = sendToServer(digits);

            myFrame.setTitle("Found digits: " + digits + ", server response:" + fromServer);

        } catch (Exception e) {
            try {
                String fromServer = sendToServer(-1);
            } catch (IOException e1) {
                System.out.println("Cannot send to server " + e1.getMessage());
                System.exit(-1);
            }
            myFrame.setTitle(e.getMessage());
        }
        DigitalReader.draw(frameMat, cropRect, new Scalar(0,0,255), 1);

        BufferedImage image = mat2Img.getImage(frameMat);

        frameMat.release();
        return image;
    }

    private String sendToServer(int digits) throws IOException {
        if (lastSendToServer==digits) {
            return digits + " not sent";
        }
        lastSendToServer = digits;
        String url = "http://www.pardubicebezobalu.cz/vaha.php?vaha="+digits;

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

// optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

//print result
        return response.toString();
    }
}