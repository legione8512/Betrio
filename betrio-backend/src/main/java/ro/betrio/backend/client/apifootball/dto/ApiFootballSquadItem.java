package ro.betrio.backend.client.apifootball.dto;

import java.util.List;

public class ApiFootballSquadItem {

    private TeamInfo team;
    private List<PlayerInfo> players;

    public TeamInfo getTeam() {
        return team;
    }

    public void setTeam(TeamInfo team) {
        this.team = team;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfo> players) {
        this.players = players;
    }

    public static class TeamInfo {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class PlayerInfo {
        private Long id;
        private String name;
        private Integer age;
        private Integer number;
        private String position;
        private String photo;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Integer getNumber() { return number; }
        public void setNumber(Integer number) { this.number = number; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public String getPhoto() { return photo; }
        public void setPhoto(String photo) { this.photo = photo; }
    }
}