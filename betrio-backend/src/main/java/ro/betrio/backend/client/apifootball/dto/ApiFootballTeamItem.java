package ro.betrio.backend.client.apifootball.dto;

public class ApiFootballTeamItem {

    private TeamInfo team;
    private VenueInfo venue;

    public TeamInfo getTeam() { return team; }
    public void setTeam(TeamInfo team) { this.team = team; }
    public VenueInfo getVenue() { return venue; }
    public void setVenue(VenueInfo venue) { this.venue = venue; }

    public static class TeamInfo {
        private Long id;
        private String name;
        private String code;
        private String country;
        private Integer founded;
        private Boolean national;
        private String logo;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public Integer getFounded() { return founded; }
        public void setFounded(Integer founded) { this.founded = founded; }
        public Boolean getNational() { return national; }
        public void setNational(Boolean national) { this.national = national; }
        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }
    }

    public static class VenueInfo {
        private String name;
        private String city;
        private Integer capacity;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
    }
}