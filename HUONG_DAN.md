# NEXUS JavaFX — chạy và đọc luồng nhắn tin

## Chạy trên máy này

1. Bật MySQL/MariaDB trong XAMPP.
2. Chạy `chay-may-chu.cmd` trong `D:\nexus-javafx`.
3. Chạy `chay-ung-dung.cmd`; có thể mở nhiều lần để thử nhiều tài khoản.
4. Client trên máy chạy server dùng `127.0.0.1`, cổng `4040`. Máy khác trong LAN dùng IP LAN của máy chạy server, đồng thời server phải được cấu hình lắng nghe địa chỉ LAN và tường lửa cho phép. Cấu hình mặc định chỉ nghe localhost.

Khi cài mới CSDL, nhập lần lượt `co-so-du-lieu/001_khoi_tao.sql` rồi `002_moc_doc.sql` rồi `003_ban_be.sql` rồi `004_ho_so_va_phien.sql` rồi `005_tep_dinh_kem.sql` rồi `006_tuong_tac_tin_nhan.sql` và `007_an_tin_nhan.sql` bằng tài khoản quản trị. Cấu hình kết nối riêng ở `cau-hinh/may-chu.properties`; không chia sẻ tệp chứa mật khẩu này. Migration 002–007 đã được áp dụng trên máy hiện tại.

## Luồng chính

```text
Client đăng nhập → server gắn socket với tài khoản đã xác thực
Client lấy danh sách người dùng + hội thoại + lịch sử phòng chung
A gửi B → server lấy danh tính A từ socket → lưu tin vào MariaDB
         → phát tinNhanMoi tới các kết nối của A và B → trả xác nhận lưu
Client B nhận tinNhanMoi → cập nhật danh sách hội thoại
                         → nếu đang mở A: thêm tin vào cửa sổ A
                         → nếu đang mở C: chỉ cập nhật dòng A và số chưa đọc
```

`layHoiThoai` trả các cuộc trò chuyện đã có tin, tin cuối (xem trước tối đa 60 ký tự Unicode) và số tin chưa đọc. Mỗi trang tối đa 50 cuộc trò chuyện; client tải tiếp các trang. Các tài khoản chưa trò chuyện vẫn xuất hiện để bắt đầu gửi tin. Cột Tin nhắn chỉ liệt kê cuộc trò chuyện đã có tin; người được mở từ mục Bạn bè cũng xuất hiện để bắt đầu trò chuyện. Mở mục Bạn bè để xem danh sách đã kết bạn; không có ô chọn bộ lọc ở cột chat.

`danhDauDoc` chỉ gửi khi đã tải được lịch sử và cửa sổ đang có tiêu điểm. Mốc đọc là ID cao nhất đã đưa vào khung trò chuyện; đổi người đang xem không đánh dấu nhầm cuộc trò chuyện cũ. Quy ước hiện tại: mở cuộc trò chuyện ở cửa sổ có tiêu điểm là đã đọc; chưa theo dõi từng bong bóng thực sự nằm trong vùng cuộn.

Server kiểm tra ID tin thuộc đúng cuộc trò chuyện và tài khoản đang đăng nhập. Bảng `moc_doc` lưu mốc theo người dùng và đối phương (0 là phòng chung), chỉ tăng, không lùi khi yêu cầu cũ đến trễ. Tin do chính mình gửi không được tính chưa đọc. Các kết nối cùng tài khoản nhận sự kiện `mocDocDoi` để làm mới trạng thái.

Khi mất kết nối, ứng dụng báo và quay về đăng nhập; đăng nhập lại sẽ lấy lịch sử và số chưa đọc từ CSDL. App giữ mã phiên, có thể khôi phục từ mục tài khoản đã đăng nhập; khi mở app lại sẽ thử khôi phục tài khoản dùng gần nhất. Chưa tự nối lại nền khi đang chat. Thông báo hiện là cập nhật trong app, chưa có thông báo hệ điều hành.

## Đọc code theo thứ tự

- `client.hatang.KhoiDong` là điểm chạy client. JavaFX gọi `UngDungNexus.start()` để mở cửa sổ.
- `client.hatang.KenhTCP` gửi yêu cầu và đối chiếu phản hồi bằng mã yêu cầu.
- `server.hatang.MayChuChat` mở cổng TCP. `server.controller.MayChuController` xử lý yêu cầu và chuyển tin đến socket tương ứng.
- `server.dao.TinNhanDao` lưu tin, truy vấn hội thoại và cập nhật mốc đọc bằng JDBC. `CSDLDao` mở kết nối cho các DAO.
- `client.view.HoiThoaiView` hiển thị hội thoại và cập nhật khi nhận sự kiện.
- `client.controller.DangNhapController` giữ kết nối tạm khi xác thực. `DangNhapView` dựng biểu mẫu và hiển thị kết quả.
- `client.model.DanhSachHoiThoaiModel` và `LichSuTinNhanModel` giữ dữ liệu chat. `AnhTinNhanController` tải ảnh, còn `AnhTinNhanModel` giữ cache ảnh.
- Gói `giaothuc` chứa mã đóng khung TCP và kiểu thông điệp dùng chung cho client và server.

Tên thư mục, lớp và hàm do dự án định nghĩa sử dụng tiếng Việt không dấu; API Java/JavaFX/JDBC giữ tên của thư viện.

## Kiểm tra

Chạy `bien-dich.cmd`: 5 kiểm thử đóng khung TCP và 17 kiểm thử server tài khoản/chat, 9 kiểm thử truyền tệp với socket thật + MariaDB cục bộ 10 kiểm thử tương tác tin nhắn và 2 kiểm thử kho phiên Windows (tổng 43). Dữ liệu kiểm thử có email riêng và được dọn sau khi chạy. Không chạy bộ kiểm thử vào CSDL sản xuất.

Lưu ý triển khai: bản này chạy trên mạng tin cậy với TCP thuần, cấu hình mặc định localhost. Gọi thoại 1–1 dùng UDP trực tiếp trong LAN, âm thanh chưa mã hóa. Gọi video và gọi qua Internet chưa được triển khai.

## Thử gọi thoại 1–1

1. Bật MySQL, chạy server và mở hai client bằng hai tài khoản. Nếu dùng hai máy, cho cả hai client kết nối bằng IP LAN của server. Server phải lắng nghe IP LAN hoặc `0.0.0.0` thay vì `127.0.0.1`.
2. Mở hội thoại riêng và bấm **Gọi thoại**. Trên máy bên kia, chọn **Bắt máy** hoặc **Từ chối** ở thanh cuộc gọi.
3. Sau khi bắt máy, app tự mở mic và loa. Thử nói qua lại, **Tắt mic** rồi **Cúp máy**. Windows có thể cần quyền mic và cho phép Java nhận UDP trên mạng riêng. Dùng tai nghe khi thử hai client trên cùng máy.

TCP chuyển lời mời, bắt máy và cúp máy. Server lấy IP từ kết nối TCP và chuyển cổng UDP do từng app tự mở. Gói UDP mang 20 ms âm thanh PCM và đi trực tiếp giữa hai client.

## Bạn bè (06/11/2026)

Nhấn tab **Bạn bè** trên thanh điều hướng sát mép trái. Nhập đúng email tài khoản khác rồi chọn **Tìm tài khoản → Kết bạn**. Người nhận thấy số lời mời ngay trên nút Bạn bè, vào thẻ **Lời mời nhận** để chấp nhận hoặc từ chối. Thẻ **Đã gửi** cho phép thu hồi; thẻ **Bạn bè** cho phép mở chat hoặc hủy kết bạn (có xác nhận). Hủy kết bạn giữ lịch sử trò chuyện.

Luồng code: `ManHinhBanBe.java` gửi `timTaiKhoan`, `layBanBe`, `thaoTacBanBe` qua `KenhTCP` → `MayChuChat.java` lấy người thao tác từ socket đã đăng nhập → `KhoBanBe.java` kiểm tra quyền và ghi SQL → server phát `banBeDoi` tới các kết nối của đúng hai tài khoản → mỗi client tải lại danh sách. Khi người nhận offline, lời mời vẫn được lưu, đăng nhập lại sẽ thấy.

Bảng `ban_be` dùng khóa chính `(nguoi_nho_id, nguoi_lon_id)` để mỗi cặp chỉ có một dòng. `nguoi_gui_id` cho biết hướng lời mời. Trạng thái SQL là `cho` hoặc `ban_be`; server trả về phía client `daGui`, `daNhan`, `ban_be` tùy tài khoản đang đăng nhập. Chỉ bên nhận được chấp nhận/từ chối; chỉ bên gửi được thu hồi. Gửi cùng lúc hai chiều giữ một lời mời, bên còn lại chọn chấp nhận rõ ràng.

Danh sách bạn bè dùng phân trang theo ID, 50 dòng/trang. Kết quả tìm kiếm chỉ có mã, tên hiển thị, online và quan hệ; không gửi email hoặc mật khẩu cho tài khoản khác. Quyền nhắn tin hiện vẫn cho phép chọn người dùng chưa kết bạn như bản trước; mục Bạn bè phục vụ tổ chức danh bạ, chưa có chức năng chặn người dùng.

## Hồ sơ và tài khoản (06/11/2026)

Bấm **ảnh đại diện ở gần góc dưới bên trái** để mở hồ sơ ở chế độ xem. Bấm **Chỉnh sửa hồ sơ** mới hiện biểu mẫu đổi tên, số điện thoại, giới thiệu và ảnh. **Lưu thay đổi** ghi xuống CSDL; **Hủy** bỏ bản nháp. Email là định danh đăng nhập, hiện chỉ đọc. Ảnh chọn PNG/JPEG tối đa 10 MB, tự cắt vuông ở giữa và nén 256×256, tối đa 32 KB trước khi gửi; có nút xóa ảnh. Giới thiệu tối đa 240 ký tự; số điện thoại 8–15 chữ số, tùy chọn dấu + ở đầu. Dữ liệu điện thoại/giới thiệu chỉ được trả trong hồ sơ của chính tài khoản.

**Đổi mật khẩu** yêu cầu mật khẩu hiện tại và nhập lại mật khẩu mới. Mật khẩu mới 8–128 ký tự, khác mật khẩu cũ. Server đổi mật khẩu và thu hồi các phiên khác trong một giao dịch; phiên đang dùng được giữ. Mã phiên dùng lại ở cùng phiên sẽ tiếp tục hợp lệ, những phiên độc lập khác phải đăng nhập lại.

Trong hồ sơ, chọn **Chuyển / thêm tài khoản** để xem những tài khoản đã đăng nhập trên máy. **Thêm tài khoản** mở trang đăng nhập, giữ phiên tài khoản trước. **Chuyển** khôi phục bằng mã phiên, không nhập lại mật khẩu. Chỉ tài khoản đang được chọn có kết nối chat; tài khoản khác giữ phiên và tải tin khi chuyển lại. Đóng app chỉ đóng socket, không đăng xuất. Khi mở lại, app thử khôi phục tài khoản được dùng gần nhất; nếu máy chủ tạm mất kết nối, phiên vẫn được giữ để thử lại.

**Đăng xuất** thu hồi phiên được chọn trên server và xóa nó khỏi danh sách đã lưu trên máy, giữ các tài khoản còn lại. Nếu máy chủ không truy cập được, app xóa phiên trên máy và báo chưa có xác nhận từ máy chủ; mã phiên phía server chưa được thu hồi trong tình huống này. Thay đổi mật khẩu có thể dùng để thu hồi những phiên khác.

Kho phiên mặc định ở `%LOCALAPPDATA%\NexusJavaFX\tai-khoan.dat`, được mã hóa bằng Windows DPAPI của người dùng Windows hiện tại. Không lưu mật khẩu. CSDL `phien_dang_nhap` chỉ lưu SHA-256 của mã phiên ngẫu nhiên 32 byte; đóng app không xóa phiên, đăng xuất hoặc đổi mật khẩu mới thu hồi theo quy tắc trên. Phiên gắn với địa chỉ máy chủ, cổng và ID tài khoản để không lẫn tài khoản giữa các server.

Phần hồ sơ gồm `HoSoView.java`, `XuLyAnh.java` và `HoSoModel.java` và `HoSoDao.java`. Phần phiên và chuyển tài khoản gồm `PhienDao.java`, `TaiKhoanView.java`, `TaiKhoanModel.java` và `UngDungNexus.java`.

Hồ sơ, chỉnh sửa và đổi mật khẩu hiển thị ở vùng nội dung của cửa sổ chính. Nút “Quay về trò chuyện” trả về cuộc trò chuyện trước; bấm một người hoặc phòng chung ở cột trái cũng quay về chat. Tin đến khi đang xem hồ sơ vẫn được tính chưa đọc.

## Gửi tệp và ảnh qua TCP (06/11/2026)

Trong khung soạn, bấm **icon kẹp giấy** (tệp) hoặc **icon hình ảnh**, chọn tệp tối đa 10 MB. Tệp xuất hiện ở bản nháp; có thể nhập chú thích rồi bấm **Gửi**, hoặc **Bỏ tệp**. Mỗi lần chọn gửi một tệp. Tiến trình và nút **Hủy truyền** nằm trong khung soạn, không mở cửa sổ phụ của app. Ảnh tự hiển thị trong chat khi xuất hiện trên màn hình, không cần bấm xem và không hiện tên tệp thay cho ảnh. Bên dưới giữ chú thích (nếu có) ; tải ảnh gốc qua chuột phải → **Tải xuống**. Chuột phải → **Tải xuống** mở hộp chọn vị trí lưu của hệ điều hành.

Luồng: client tính SHA-256 → `tepBatDau` gửi tên, kích thước, SHA, người nhận và UUID gửi → `tepGuiKhoi` gửi tuần tự từng khối tối đa 24.576 byte (Base64 trong khung JSON TCP) → server xác nhận vị trí đã nhận → `tepKetThuc` kiểm tra đủ dung lượng và đúng SHA-256 → ghi metadata/tin nhắn và phát `tinNhanMoi`. Chỉ lúc hoàn tất mới tạo tin trong lịch sử. Đây là giao thức ứng dụng tự xây dựng trên TCP, không phải FTP.

Server lưu bytes tại `du-lieu/tep` dưới tên UUID `.bin`, không dùng tên tệp người gửi làm đường dẫn. CSDL lưu metadata và liên kết với tin nhắn trong bảng `tep_dinh_kem`; không lưu toàn bộ tệp lớn trong CSDL. PNG/JPEG hợp lệ có thể có ảnh thu JPEG tối đa 24 KB, cạnh dài tối đa 320px; ảnh không tạo được bản thu vẫn tải được như tệp thông thường. Tên, dung lượng và chú thích vẫn có trong lịch sử khi đăng nhập lại. Khi sao lưu hệ thống, cần sao lưu cả CSDL và thư mục `du-lieu/tep`.

Tải tệp dùng `tepThongTin` và `tepDocKhoi`, mỗi yêu cầu kiểm tra quyền tài khoản trên server. Tin riêng chỉ người gửi và người nhận được tải; phòng chung cho phép mọi tài khoản đã đăng nhập. Client ghi vào tệp `.part` cạnh vị trí lưu, kiểm tra SHA-256 rồi mới thay thế tệp đích. Hủy/tải lỗi giữ nguyên tệp đích cũ. Không tự mở hoặc thực thi tệp tải về.

Hủy hoặc ngắt socket sẽ dọn tệp đang nhận dở. Chưa hỗ trợ nối tiếp phần đang dở sau khi mất mạng; chọn lại để gửi từ đầu. Gửi lại cùng mã UUID, tên, nội dung và SHA sẽ trả về tin đã lưu để tránh trùng khi mất phản hồi. Đóng/mở app không lưu bản nháp UUID gửi tệp; có thể cần kiểm tra lịch sử trước khi gửi lại sau một lần mất kết nối vào thời điểm hoàn tất. Thư mục tệp lưu sau khi máy chủ dừng đột ngột chưa có tác vụ tự dọn tệp mồ côi.

Một client truyền một tệp tại một thời điểm; phần I/O và tính SHA chạy nền, tin chat vẫn sử dụng được cùng kết nối. Server giới hạn lệnh nghiệp vụ 160/phút/kết nối và lệnh khối 1.200/phút/kết nối; tệp không được đưa nguyên khối vào một khung TCP. Lịch sử phân trang thêm theo số byte để tin dài và metadata tệp không vượt giới hạn khung 64 KB.

Đọc code: `TruyenTepTCP.java` (client chia/ghép khối, hủy và SHA), `BoNhanTep.java` (trạng thái nhận từng socket), `KhoTep.java` (ổ đĩa, SQL, quyền tải, ảnh thu), `TheTep.java` (thẻ và xem ảnh trong chat), `TruyenTepTest.java` (kiểm thử giao thức). `kiem-tra/KiemTraTep.java` là kiểm tra app thật bằng tài khoản thử, có kiểm tra hủy tải không ghi đè tệp cũ.

Các trang Bạn bè, tài khoản đã lưu và hồ sơ đều hiển thị trong cửa sổ chính. Xác nhận hủy kết bạn là hộp nổi phủ trên trang Bạn bè; biểu cảm mở ngay trong khung soạn. Chỉ hộp chọn tệp/ảnh và nơi lưu của hệ điều hành mở riêng.

Khung soạn dùng icon nét vector cho biểu cảm, kẹp giấy, hình ảnh và gửi. Di chuột để xem nhãn; các nút có tên truy cập bàn phím. Biểu cảm trong khung soạn chèn emoji vào nội dung; cảm xúc gắn với từng tin nằm cạnh tin nhắn như mô tả dưới đây.


## Trả lời, thu hồi và cảm xúc (06/11/2026)

- **Trả lời:** bấm chuột phải tin nhắn → **Trả lời**. Thanh trích dẫn xuất hiện trên ô soạn; nhập tin hoặc chọn tệp rồi gửi. Nút × bỏ trả lời. Đổi cuộc trò chuyện bỏ trích dẫn để không gửi nhầm.
- **Cảm xúc:** bấm chuột phải → **Thả cảm xúc** rồi chọn 👍, ❤️, 😂, 😮, 😢 hoặc 😡. Mỗi tài khoản có một cảm xúc trên một tin; chọn cảm xúc khác để đổi, bấm lại cảm xúc đã chọn để bỏ. Các chip hiển thị tổng số người chọn và đánh dấu cảm xúc của mình.
- **Thu hồi:** bấm chuột phải tin của mình → **Thu hồi**, rồi xác nhận **Thu hồi** trong hộp nổi giữa màn hình; **Giữ lại** đóng phần xác nhận. Tin được thay bằng “Tin nhắn đã được thu hồi”; không đặt giới hạn thời gian trong bản hiện tại. Server chỉ cho chủ tin thu hồi.

Áp dụng cho phòng chung và chat riêng, tin chữ và tệp/ảnh. Tin đã thu hồi không còn nút trả lời/cảm xúc/tải tệp. Các trích dẫn của tin gốc cũng hiển thị trạng thái thu hồi, kể cả khi tải lại lịch sử. Nếu đang chọn trả lời một tin vừa bị thu hồi, app bỏ trích dẫn và giữ nội dung đang gõ. Thu hồi chặn các yêu cầu tải tiếp trên server và dọn bytes tệp; bản đã tải/lưu/chụp ở máy khác không thể tự xóa khỏi máy đó.

Luồng code:

```text
BongTinNhan → ManHinhHoiThoai.thaoTacTin → KenhTCP
  → MayChuChat (danh tính từ socket đã đăng nhập)
  → KhoTuongTacTinNhan (quyền truy cập, giao dịch SQL)
  → tinNhanDoi tới đúng hai tài khoản, hoặc cả phòng chung
  → client cập nhật tin và trích dẫn, giữ nguyên cuộc trò chuyện đang mở
```

Trả lời gửi `traLoiId` trong `guiTin` hoặc `tepBatDau`. Server tự đọc nội dung trích dẫn từ CSDL, không tin nội dung trích dẫn do client khai báo; chỉ cho trích dẫn trong cùng cuộc trò chuyện. `thuHoiTin` nhận `tinNhanId`; `datCamXuc` nhận `tinNhanId` và `bieuCam` (chuỗi rỗng để bỏ). Tin trả về có `daThuHoi`, `phienBan`, `trichDan`, `camXuc`, `camXucCuaToi`.

Migration 006 thêm liên kết tin được trả lời, cờ thu hồi, phiên bản và SHA-256 nội dung gốc trong `tin_nhan`; bảng `cam_xuc_tin_nhan` có khóa chính `(tin_nhan_id, nguoi_dung_id)`. Thu hồi xóa nội dung gốc đang hiển thị và cảm xúc trong một giao dịch; giữ SHA-256 để gửi lại cùng UUID không khôi phục tin hoặc tạo bản trùng. Phiên bản tăng khi đổi cảm xúc/thu hồi; client dùng để bỏ phản hồi cũ đến trễ. Tin thu hồi không được tính vào số chưa đọc. Server tuần tự hóa thao tác tin và đọc lịch sử để phát các bản cập nhật theo thứ tự.

`TuongTacTinNhanTest.java` kiểm tra với socket TCP và MariaDB: quyền truy cập, đúng cuộc trò chuyện, gửi lại UUID, đổi/bỏ cảm xúc, thu hồi/ẩn trích dẫn, chặn tải tệp, sự kiện đúng người, tải lại lịch sử và thao tác đồng thời. `kiem-tra/KiemTraTuongTac.java` bấm các nút thật trong JavaFX, xác nhận chỉ một cửa sổ, giữ bản nháp và không để phản hồi cũ phục hồi tin đã thu hồi. Ảnh kiểm tra nằm trong `nhat-ky/kiem-tra`.

Ảnh trong chat dùng ảnh thu tối đa 24 KB; tải nền tối đa 3 ảnh cùng lúc, gộp các yêu cầu trùng khi vẽ lại tin và giữ tối đa 128 ảnh trong bộ nhớ của tài khoản. Cuộn tới ảnh cũ sẽ tự tải ảnh đó. Lỗi mạng hiện **Thử lại** ngay trên ô ảnh; thu hồi tin xóa ảnh khỏi chat và bộ nhớ đệm.


## Menu chuột phải của tin nhắn (06/11/2026)

Các nút tải, trả lời, thả cảm xúc và thu hồi không còn hiển thị thường trực. Bấm chuột phải vào bong bóng, ảnh hoặc tệp để mở menu **Sao chép**, **Tải xuống** (ảnh/tệp), **Chuyển tiếp**, **Trả lời**, **Thả cảm xúc**, **Thu hồi** (tin của mình) và **Xóa ở phía tôi**. Menu là menu ngữ cảnh; biểu mẫu chuyển tiếp nằm trong cửa sổ chính; xác nhận dùng lớp phủ nổi trong cùng cửa sổ.

**Chuyển tiếp** mở phần chọn tài khoản hoặc phòng chung; bấm nút gửi mới chuyển tin. Server đọc tin gốc có kiểm tra quyền, không nhận nội dung gốc tự khai báo từ client. Tin chữ tạo tin mới; tệp/ảnh được sao chép sang tệp độc lập với quyền của cuộc trò chuyện mới. Thu hồi bản gốc không thu hồi các bản đã chuyển tiếp. Không chuyển tiếp được tin đã thu hồi.

**Xóa ở phía tôi** có xác nhận, lưu trong bảng `tin_da_an` của migration 007. Lịch sử, xem trước hội thoại và số chưa đọc của tài khoản đó bỏ qua tin đã xóa; những người khác vẫn thấy tin. Các kết nối của cùng tài khoản nhận `tinBiXoa`. Thu hồi vẫn là thao tác của người gửi, tác động tới mọi người trong cuộc trò chuyện.


## Điều hướng và xác nhận nổi (06/11/2026)

Thanh sát mép trái có **Tin nhắn**, **Bạn bè** (kèm số lời mời), **ảnh hồ sơ/tài khoản** ở gần dưới và **Đăng xuất**. Tab Bạn bè được đưa ra khỏi cột hội thoại. Tin nhắn giữ cuộc trò chuyện đang mở; Bạn bè mở danh bạ/tìm bằng email/lời mời, đồng thời ẩn cột hội thoại. Quay lại Tin nhắn sẽ hiện lại danh sách và bản nháp cũ.

Thu hồi/xóa tin, thu hồi lời mời, hủy kết bạn và đăng xuất hỏi bằng hộp nổi giữa màn hình, nền mờ và bị khóa thao tác. **Hủy** hoặc Escape đóng xác nhận; nút xác nhận mới thực hiện. Không tạo Stage hoặc cửa sổ hệ điều hành. `ThongBaoNoi.java` dùng chung lớp phủ trong Scene hiện tại; thao tác tin chờ kết quả server và hiển thị lỗi trong hộp nếu thất bại. Đăng xuất được hỏi từ cả thanh trái và danh sách tài khoản đã lưu.


## Trích dẫn ảnh, bản nháp và chia sẻ (06/11/2026)

Trả lời một tin ảnh hiển thị ảnh thu nhỏ và chú thích (hoặc nhãn “Ảnh” khi ảnh không có chú thích), cả trên thanh trả lời và trong tin đã gửi. Server trả `loaiTin` trong `trichDan` để lịch sử vẫn nhận diện ảnh khi tin gốc không có trong trang đang tải. Tin thu hồi thay trích dẫn bằng trạng thái thu hồi.

Khi chọn ảnh, ảnh xem trước nằm ngay trong khung nhập trước khi gửi; **Bỏ tệp** bỏ ảnh đang chọn. Đọc ảnh xem trước chạy nền. Ô nhập mặc định một dòng, tăng chiều cao khi xuống dòng; bong bóng tin ngắn không bị kéo rộng theo trích dẫn phía trên.

Chuột phải → **Chuyển tiếp** mở hộp **Chia sẻ** có ô tìm kiếm, các mục **Gần đây**, **Bạn bè**, **Phòng chung** và ô chọn từng nơi nhận. Gần đây lấy từ các cuộc trò chuyện đã có tin, sắp theo tin gần nhất. Có thể chọn nhiều nơi, nhập lời nhắn kèm theo và bấm **Chia sẻ (n)**. Lời nhắn được gửi thành tin chữ sau tin chuyển tiếp tại từng nơi nhận. Hủy/Escape đóng hộp khi chưa gửi; trong lúc gửi khóa các lựa chọn. Nếu một nơi gửi lỗi, giữ các nơi chưa hoàn tất để thử lại với mã gửi cũ, tránh nhân đôi tin đã chuyển tiếp.

Luồng giao diện tách trong `TrichDanTin.java` và `ManHinhChiaSe.java`; cùng dùng `KhoAnhTinNhan` và lớp phủ `ThongBaoNoi`. Không tạo cửa sổ ứng dụng khác.


## Thông tin chat, ghim tin và quyền riêng tư (06/11/2026)

Áp dụng thêm `co-so-du-lieu/008_ghim_tin.sql` và `009_quyen_rieng_tu.sql` sau các migration trước. Máy hiện tại đã áp dụng; khi chuyển máy cần import theo thứ tự. Biên dịch lại và khởi động lại server/client.

Trong cuộc trò chuyện, **Thông tin** mở kho ảnh–tệp (phân trang 20 mục) và danh sách tin đã ghim. Chuột phải tin → **Ghim tin nhắn**; vào Tin đã ghim để bỏ ghim. Kho này là tập hợp ảnh/tệp trong hội thoại, không phải thao tác ẩn/lưu trữ hội thoại. Tin đã thu hồi hoặc đã xóa ở phía người xem không xuất hiện trong kho. Cả hai người tham gia có thể ghim/bỏ ghim; phòng chung cho các thành viên đang đăng nhập.

Bấm tên ở đầu hội thoại hoặc **Hồ sơ** trong danh sách bạn bè để xem trang cá nhân. Nhãn đầu hội thoại là **Bạn bè** hoặc **Người lạ**. Hồ sơ cá nhân → **Quyền riêng tư** cho phép chọn riêng ai xem email và số điện thoại: Mọi người / Bạn bè / Chỉ mình tôi. Mặc định Chỉ mình tôi; lưu trong CSDL. Server lọc trường bị ẩn trước khi trả dữ liệu. Hủy kết bạn sẽ mất quyền xem trường dành cho bạn bè ở lần tải hồ sơ tiếp theo. Cài đặt này điều khiển việc xem hồ sơ, không chặn tìm kiếm bằng email đã biết. Thông tin đã được xem trước khi đổi quyền không thể thu hồi khỏi trí nhớ/ảnh chụp của người xem.

Tin riêng của mình hiển thị Đã gửi sau khi server lưu; Đã xem khi đối phương gửi mốc đọc. Client chỉ đánh dấu đọc khi cửa sổ có tiêu điểm, mở đúng hội thoại và đang ở cuối. Khi mở hội thoại hoặc đang ở cuối có tin mới, tự cuộn về cuối. Đang đọc tin cũ giữ vị trí và hiện **nút mũi tên xuống**. Giao diện tái sử dụng bong bóng không đổi, tránh dựng lại toàn bộ ảnh/tin mỗi lần cập nhật. Danh sách hiện vẫn dùng VBox có phân trang, chưa phải danh sách ảo hóa cho lịch sử cực lớn.

Kiểm tra: 45 ca Maven thành công; `kiem-tra/KiemTraTuongTac.java` kiểm tra TCP/CSDL thật bằng tài khoản thử, hồ sơ/quyền riêng tư cùng cửa sổ, giữ đối tượng bong bóng và vị trí cuộn khi tin mới đến, nút về cuối, các thao tác trả lời/chuyển tiếp/thu hồi/xóa.

Giao diện quyền riêng tư dùng ba lựa chọn trực tiếp trên một hàng thay ô xổ mặc định. Nút về cuối hội thoại chỉ có icon vector, có nhãn hỗ trợ đọc màn hình và chú thích khi rê chuột. Khi đổi hội thoại, bỏ qua phản hồi mốc đọc và lệnh cuộn cuối đã xếp hàng của lượt tải cũ; mốc đã xem không giảm khi phản hồi đến trễ.


## Đối chiếu Android NEXUS: ghim, thông tin chat, hồ sơ (06/11/2026)

Đã đọc bản gốc `D:/AndroidStudioProjects/NEXUS/app/src/main/java/com/example/nexus/`:
- `feature_chat/ui/ChatComponents.kt`: PinnedMessageBar, tên người gửi, nội dung, bấm tới tin gốc và bỏ ghim.
- `feature_chat/ui/ConversationScreen.kt`: thanh ghim bên dưới đầu hội thoại, định vị tin nhắn.
- `feature_chat/ui/ChatInfoScreen.kt`: ảnh/tên ở đầu, nội dung chia sẻ phân loại, liên kết tới trang cá nhân.
- `feature_chat/ui/SharedMediaScreen.kt`: lưới ảnh và danh sách tệp/liên kết.
- `feature_contact/ui/OtherUserProfileScreen.kt`: hành động theo quan hệ bạn bè.

JavaFX đã áp dụng các luồng trên: thanh ghim nổi dưới đầu chat, tin vừa ghim hiển thị trước; bấm tới tin gốc (tải thêm lịch sử nếu cần) và tô sáng 2 giây. Bỏ ghim có xác nhận trong cùng cửa sổ. JavaFX giữ danh sách nhiều tin ghim đã có, trong khi Android hiện dùng một pinnedMessage. Migration `010_thoi_diem_ghim.sql` ghi thời điểm ghim; máy hiện tại đã áp dụng.

Thông tin chat hiển thị ảnh đại diện/tên/quan hệ và nút mở hồ sơ. Kho chia thành Ảnh, Tệp, Liên kết, Tin đã ghim; ảnh dạng lưới, bấm đưa về ảnh trong chat; tệp có tải xuống và đi tới tin; liên kết hiển thị tin chứa http/https, đi tới tin gốc để xem đầy đủ. Chưa có trình xem video hoặc trình xem ảnh toàn màn hình như Android.

Hồ sơ người khác có Nhắn tin; thêm Kết bạn, Thu hồi lời mời, Chấp nhận/Từ chối, Hủy kết bạn theo quan hệ thực tế. Xác nhận hủy kết bạn/thu hồi lời mời dùng lớp phủ. Email/số điện thoại vẫn được máy chủ lọc theo quyền riêng tư. JavaFX cho phép nhắn tin với người lạ như hành vi hiện có, không áp dụng giới hạn chỉ bạn bè của Android.

Đây là đối chiếu ba phần yêu cầu, không phải đã chuyển toàn bộ Android: gọi thoại/video, chặn người dùng, biệt danh, đổi chủ đề, tắt thông báo, chia sẻ liên hệ và lưu trữ cuộc trò chuyện của Android chưa được triển khai trong lần này. Đặc biệt Lưu trữ cuộc trò chuyện là chức năng riêng, không đồng nghĩa kho ảnh/tệp.

### Điều hướng theo Android

Thông tin, Nội dung chia sẻ và Hồ sơ mở thành các trang trong cùng cửa sổ. Nút mũi tên quay lại trang trước. Mỗi cuộc trò chuyện có một tin ghim đang hoạt động. Ghim tin khác thay thế tin hiện tại.

Trong Thông tin, bấm Lưu trữ cuộc trò chuyện để chuyển chat riêng sang mục Lưu trữ của tài khoản. Mở chat từ mục đó và vào Thông tin để bỏ lưu trữ. Khi chuyển máy, chạy thêm migration `011_hoi_thoai_luu_tru.sql` trước khi khởi động máy chủ.

Bấm tên ở đầu chat để mở bảng thông tin bên phải. Kho ảnh, tệp và liên kết có bản xem trước. Mục Lưu trữ nằm ở thanh menu bên trái. Chuột phải vào hội thoại để lưu trữ hoặc bỏ lưu trữ.

Mở chat riêng, bấm tên người kia rồi kéo xuống cuối bảng thông tin hội thoại. **Chặn** nằm cạnh **Xóa cuộc trò chuyện**. Khi chặn, cả hai bên mất ô nhập tin và không thể gọi cho nhau. Bên bị chặn thấy **Không thể gửi tin nhắn**. Bên chặn có thể bấm **Bỏ chặn** ngay trong hội thoại. Xóa cuộc trò chuyện chỉ ẩn lịch sử hiện có ở phía tài khoản thao tác. Khi chuyển máy, áp dụng thêm migration `012_chan_nguoi_dung.sql`.

### Chat nhóm

Áp dụng `013_nhom_chat.sql` trước khi khởi động bản máy chủ có chat nhóm. Trong Tin nhắn, chọn Tạo nhóm, nhập tên và chọn từ 1 đến 29 bạn bè. Nhóm xuất hiện trong danh sách hội thoại của các thành viên. Bản đầu hỗ trợ tin chữ qua TCP, lịch sử phân trang và nhận tin trực tiếp. Chỉ thành viên mới xem hoặc gửi được tin trong nhóm.

Hộp Tạo nhóm có thể chọn ảnh và lọc bạn bè. Khi chuyển máy, áp dụng thêm migration `014_anh_nhom.sql`. Mục Bạn bè nhận email hoặc số điện thoại. Người lạ chỉ tìm bằng số được khi chủ tài khoản đặt quyền xem số điện thoại là Mọi người.

### Lịch sử cuộc gọi thoại

Nhập `015_lich_su_cuoc_goi.sql` sau migration 014 bằng tài khoản quản trị trong phpMyAdmin, rồi khởi động lại server và client. Các cuộc gọi mới xuất hiện trực tiếp trong chat riêng, kèm kết quả và nút Gọi lại. Server ghi thời lượng từ khi người nhận bắt máy. Lời mời hết hạn hoặc cuộc gọi tới tài khoản ngoại tuyến được lưu là Cuộc gọi nhỡ. Chuột phải vào mục cuộc gọi để xóa ở phía mình.

### Gọi video 1–1 trong LAN

Áp dụng migration `016_goi_video.sql` sau 015, biên dịch và khởi động lại server cùng hai client. Mở chat riêng, chọn **Gọi video**. Người nhận thấy rõ lời mời video, có thể tắt camera trước khi bắt máy. Camera chỉ được mở sau khi lời mời được chấp nhận. Nút camera đóng thiết bị khi tắt và mở lại khi bật. Khung nhỏ hiển thị hình của mình, khung chính hiển thị đối phương.

Client dùng webcam-capture 0.3.12 để truy cập camera. Phần chia JPEG 320×240 thành gói, truyền bằng DatagramSocket, kiểm tra token phiên, ghép gói đến lệch thứ tự và bỏ khung cũ nằm ở `KhungHinhUDP` và `VideoUDP`. Tốc độ gửi mục tiêu khoảng 12 khung/giây, âm thanh chạy trên socket UDP riêng. Khi camera không sẵn sàng, cửa sổ báo lỗi và vẫn giữ âm thanh. TCP trao đổi cổng video sau khi bắt máy. Lịch sử lưu loại cuộc gọi để nút Gọi lại chọn đúng thoại hoặc video.

Đây là bản truyền JPEG qua UDP cho LAN. Chất lượng cần kiểm tra trên hai máy thật, chưa có codec video thích nghi hoặc kết nối qua NAT cho hai mạng Internet khác nhau.

### Tin nhắn thoại

Áp dụng `017_tin_nhan_thoai.sql` sau migration 016, biên dịch rồi khởi động lại server và client. Trong chat riêng hoặc phòng chat chung, bấm biểu tượng micro ở ô soạn. Bản ghi tối đa 60 giây. Bấm **Dừng** để nghe lại, bỏ hoặc gửi. Người nhận chọn **Phát** ngay trong tin nhắn.

### Cấu trúc MVC

- `client/src/main/java/client/view`, `client/src/main/java/client/controller`, `client/src/main/java/client/model` chứa mã JavaFX, điều khiển và dữ liệu máy khách. Ví dụ `TaoNhomView.java`, `NhomController.java` và `TaiKhoanModel.java`.
- `server/src/main/java/server/controller` nhận lệnh, `server/src/main/java/server/model` xử lý dữ liệu. Ví dụ `MayChuController.java` và `NhomChatModel.java`.
- `client/src/main/java/client/hatang` và `server/src/main/java/server/hatang` chứa điểm chạy và mã kết nối. Mỗi module dùng thư mục nguồn mặc định src/main/java của Maven.
