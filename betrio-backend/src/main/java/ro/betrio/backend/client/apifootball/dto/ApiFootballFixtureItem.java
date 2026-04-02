package ro.betrio.backend.client.apifootball.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
public class ApiFootballFixtureItem {

    private FixtureInfo fixture;
    private LeagueInfo league;
    private TeamsInfo teams;
    private GoalsInfo goals;
    private ScoreInfo score;

    public FixtureInfo getFixture() { return fixture; }
    public void setFixture(FixtureInfo fixture) { this.fixture = fixture; }
    public LeagueInfo getLeague() { return league; }
    public void setLeague(LeagueInfo league) { this.league = league; }
    public TeamsInfo getTeams() { return teams; }
    public void setTeams(TeamsInfo teams) { this.teams = teams; }
    public GoalsInfo getGoals() { return goals; }
    public void setGoals(GoalsInfo goals) { this.goals = goals; }
    public ScoreInfo getScore() { return score; }
    public void setScore(ScoreInfo score) { this.score = score; }

    public static class FixtureInfo {
        private Long id;
        private String referee;
        private String timezone;
        private String date;
        private StatusInfo status;
        private VenueInfo venue;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getReferee() { return referee; }
        public void setReferee(String referee) { this.referee = referee; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public StatusInfo getStatus() { return status; }
        public void setStatus(StatusInfo status) { this.status = status; }
        public VenueInfo getVenue() { return venue; }
        public void setVenue(VenueInfo venue) { this.venue = venue; }

        public static class StatusInfo {

            @JsonProperty("long")
            private String longValue;

            @JsonProperty("short")
            private String shortValue;

            private Integer elapsed;

            public String getLongValue() { return longValue; }
            public void setLongValue(String longValue) { this.longValue = longValue; }

            public String getShortValue() { return shortValue; }
            public void setShortValue(String shortValue) { this.shortValue = shortValue; }

            public Integer getElapsed() { return elapsed; }
            public void setElapsed(Integer elapsed) { this.elapsed = elapsed; }
        }

        public static class VenueInfo {
            private String name;
            private String city;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getCity() { return city; }
            public void setCity(String city) { this.city = city; }
        }
    }

    public static class LeagueInfo {
        private String round;

        public String getRound() { return round; }
        public void setRound(String round) { this.round = round; }
    }

    public static class TeamsInfo {
        private Side home;
        private Side away;

        public Side getHome() { return home; }
        public void setHome(Side home) { this.home = home; }
        public Side getAway() { return away; }
        public void setAway(Side away) { this.away = away; }

        public static class Side {
            private Long id;
            private Boolean winner;

            public Long getId() { return id; }
            public void setId(Long id) { this.id = id; }
            public Boolean getWinner() { return winner; }
            public void setWinner(Boolean winner) { this.winner = winner; }
        }
    }

    public static class GoalsInfo {
        private Integer home;
        private Integer away;

        public Integer getHome() { return home; }
        public void setHome(Integer home) { this.home = home; }
        public Integer getAway() { return away; }
        public void setAway(Integer away) { this.away = away; }
    }

    public static class ScoreInfo {
        private GoalPair halftime;
        private GoalPair fulltime;
        private GoalPair extratime;
        private GoalPair penalty;

        public GoalPair getHalftime() { return halftime; }
        public void setHalftime(GoalPair halftime) { this.halftime = halftime; }
        public GoalPair getFulltime() { return fulltime; }
        public void setFulltime(GoalPair fulltime) { this.fulltime = fulltime; }
        public GoalPair getExtratime() { return extratime; }
        public void setExtratime(GoalPair extratime) { this.extratime = extratime; }
        public GoalPair getPenalty() { return penalty; }
        public void setPenalty(GoalPair penalty) { this.penalty = penalty; }

        public static class GoalPair {
            private Integer home;
            private Integer away;

            public Integer getHome() { return home; }
            public void setHome(Integer home) { this.home = home; }
            public Integer getAway() { return away; }
            public void setAway(Integer away) { this.away = away; }
        }
    }
}