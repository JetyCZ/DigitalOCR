package net.pardubicebezobalu.scale.camera;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class MyFrame extends JFrame {
    private static int cameraIdx;
    private JPanel contentPane;

    static int x, y, width, height;

    public static void main(String[] args) {
        x = Integer.parseInt(args[0]);
        y = Integer.parseInt(args[1]);
        width = Integer.parseInt(args[2]);
        height = Integer.parseInt(args[3]);
        cameraIdx = Integer.parseInt(args[4]);
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MyFrame frame = new MyFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public MyFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0, 0, 1280, 720);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        new MyThread().start();
    }

    VideoCap videoCap = new VideoCap(cameraIdx);

    public void paint(Graphics g){
        videoCap.setMyFrame(this);
        g = contentPane.getGraphics();
        BufferedImage oneFrame = videoCap.getOneFrame();
        g.drawImage(oneFrame, 0, 0, this);
    }

    class MyThread extends Thread{
        @Override
        public void run() {
            for (;;){
                repaint();
                try { Thread.sleep(30);
                } catch (InterruptedException e) {    }
            }
        }
    }
}