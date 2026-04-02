package ro.betrio.backend.client.apifootball.dto;

public class ApiFootballLeagueItem {

    private League league;
    private Country country;
    private SeasonInfo[] seasons;

    public League getLeague() { return league; }
    public void setLeague(League league) { this.league = league; }
    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }
    public SeasonInfo[] getSeasons() { return seasons; }
    public void setSeasons(SeasonInfo[] seasons) { this.seasons = seasons; }

    public static class League {
        private Long id;
        private String name;
        private String type;
        private String logo;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }
    }

    public static class Country {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class SeasonInfo {
        private Integer year;
        private String start;
        private String end;
        private Boolean current;

        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }
        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
        public Boolean getCurrent() { return current; }
        public void setCurrent(Boolean current) { this.current = current; }
    }
}