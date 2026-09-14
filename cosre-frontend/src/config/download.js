// Kích hoạt trình duyệt tải xuống một Blob (ví dụ file .xlsx trả về từ backend) với tên file chỉ định.
export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}
