package co.wethinkcode.trafficflow;

public class Intersection
{
    private String id;
    private String district;
    private String signalType;
    private boolean active;

    public Intersection(String id, String district, String signalType, boolean active)
    {
        this.id = id;
        this.district = district;
        this.signalType = signalType;
        this.active = active;
    }

    public String getId() {return id;}
    public String getDistrict() {return district;}
    public String getSignalType() {return signalType;}
    public boolean isActive() {return active;}

    public String toJson()
    {
        String districtVal = (district == null) ? "null" : "\"" + district + "\"";
        String signalVal = (signalType == null) ? "null" : "\"" + signalType + "\"";
        return "{\"id\":\"" + id + "\",\"district\":" + districtVal + ",\"signalType\":" + signalVal + ",\"active\":" + active + "}";
    }
}
