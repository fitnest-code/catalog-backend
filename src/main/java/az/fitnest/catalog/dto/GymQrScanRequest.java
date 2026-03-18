package az.fitnest.catalog.dto;

public class GymQrScanRequest {
    private String qrCodeValue;
    private Double lat;
    private Double lng;

    public String getQrCodeValue() {
        return qrCodeValue;
    }
    public void setQrCodeValue(String qrCodeValue) {
        this.qrCodeValue = qrCodeValue;
    }
    public Double getLat() {
        return lat;
    }
    public void setLat(Double lat) {
        this.lat = lat;
    }
    public Double getLng() {
        return lng;
    }
    public void setLng(Double lng) {
        this.lng = lng;
    }
}
