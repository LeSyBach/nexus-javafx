package server.hatang;

import server.controller.MayChuController;
import server.dao.CSDLDao;

import giaothuc.*;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MayChuChat implements AutoCloseable {
    private final MayChuController dieuKhien;
    private final ServerSocket serverSocket;
    private final ThreadPoolExecutor nhomLuong =
            new ThreadPoolExecutor(0, 32, 60, TimeUnit.SECONDS, new SynchronousQueue<>());
    private final AtomicBoolean daDong = new AtomicBoolean();

    public MayChuChat(CSDLDao kho, String host, int port) throws IOException {
        this(kho, host, port, Path.of("du-lieu", "tep"));
    }

    public MayChuChat(CSDLDao kho, String host, int port, Path thuMucTep) throws IOException {
        dieuKhien = new MayChuController(kho, thuMucTep);

        this.serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        InetSocketAddress serverAddress = new InetSocketAddress(host, port);
        serverSocket.bind(serverAddress);
    }

    public int cong() {
        return serverSocket.getLocalPort();
    }

    public int soKetNoi() {
        return dieuKhien.soKetNoi();
    }

    public void phucVu() throws IOException {
        while (!daDong.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true);
                clientSocket.setKeepAlive(true);
                clientSocket.setSoTimeout(15000);

                XuLyClient client = new XuLyClient(clientSocket);
                dieuKhien.them(client);
                try {
                    nhomLuong.execute(client);
                } catch (RejectedExecutionException loi) {
                    client.close();
                }
            } catch (SocketException loi) {
                if (!daDong.get()) {
                    throw loi;
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (!daDong.compareAndSet(false, true)) {
            return;
        }
        serverSocket.close();
        dieuKhien.dongTatCa();
        nhomLuong.shutdownNow();
    }

    private final class XuLyClient extends MayChuController.Phien implements Runnable {
        private final Socket clientSocket;
        private final BlockingQueue<ThongDiep> hangGui = new ArrayBlockingQueue<>(64);
        private final AtomicBoolean dong = new AtomicBoolean();
        private int soKhoi;
        private Thread luongGui;
        private long batDauKy = System.currentTimeMillis();
        private int soYeuCau;

        private XuLyClient(Socket clientSocket) {
            dieuKhien.super();
            this.clientSocket = clientSocket;
        }

        @Override
        protected String diaChi() {
            return clientSocket.getInetAddress().getHostAddress();
        }

        @Override
        protected void datHanDoc(int miliGiay) throws SocketException {
            clientSocket.setSoTimeout(miliGiay);
        }

        @Override
        public void run() {
            luongGui = new Thread(this::guiTrongNen, "nexus-gui-" + clientSocket.getPort());
            luongGui.setDaemon(true);
            luongGui.start();

            try (DataInputStream inputStream = new DataInputStream(
                    new BufferedInputStream(clientSocket.getInputStream()))) {
                while (!dong.get()) {
                    ThongDiep yeuCau = GiaoThuc.doc(inputStream);
                    if (yeuCau.ma().isEmpty()) {
                        throw new IOException("Thiếu mã yêu cầu.");
                    }

                    if (System.currentTimeMillis() - batDauKy > 60000) {
                        batDauKy = System.currentTimeMillis();
                        soYeuCau = 0;
                        soKhoi = 0;
                    }

                    boolean laKhoi =
                            yeuCau.loai().equals("tepGuiKhoi") || yeuCau.loai().equals("tepDocKhoi");
                    if (laKhoi ? ++soKhoi > 1200 : ++soYeuCau > 160) {
                        thatBai(yeuCau, "quaNhanh", "Bạn thao tác quá nhanh. Hãy thử lại sau.");
                        continue;
                    }

                    xuLy(yeuCau);
                }
            } catch (IOException loi) {
                // Đóng phiên này khi socket ngắt, hết thời gian chờ hoặc nhận khung lỗi.
            } finally {
                close();
            }
        }

        @Override
        protected void gui(ThongDiep thongDiep) {
            if (!dong.get() && !hangGui.offer(thongDiep)) {
                close();
            }
        }

        private void guiTrongNen() {
            try (DataOutputStream outputStream = new DataOutputStream(
                    new BufferedOutputStream(clientSocket.getOutputStream()))) {
                while (!dong.get()) {
                    GiaoThuc.ghi(outputStream, hangGui.take());
                }
            } catch (IOException | InterruptedException loi) {
                Thread.currentThread().interrupt();
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            if (!dong.compareAndSet(false, true)) {
                return;
            }
            dieuKhien.roi(this, !daDong.get());
            try {
                clientSocket.close();
            } catch (IOException loi) { /* Đã đóng. */ }
            if (luongGui != null) {
                luongGui.interrupt();
            }
        }
    }

    public static void main(String[] cacThamSo) throws Exception {
        CauHinh cauHinh =
                CauHinh.doc(Path.of(cacThamSo.length == 0 ? "cau-hinh/may-chu.properties" : cacThamSo[0]));
        CSDLDao kho = new CSDLDao(cauHinh);
        kho.kiemTra();
        try (MayChuChat mayChu = new MayChuChat(kho, cauHinh.diaChi(), cauHinh.cong())) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    mayChu.close();
                } catch (IOException loi) { /* Đã dừng. */ }
            }));
            System.out.println("NEXUS Java TCP đang nghe tại " + cauHinh.diaChi() + ":" + mayChu.cong());
            mayChu.phucVu();
        }
    }
}
