package com.konrad;

import java.util.*;

public class WeArePlayingFootball {


    private final static Random RANDOM = new Random();


    public static void main(String[] args) {
        final AtomicTeamPicker atomicTeamPicker = new AtomicTeamPicker();
        playRound(atomicTeamPicker, 4);


    }

    private static Team playRound(AtomicTeamPicker atomicTeamPicker, int roundLevel) {
        Team teamA;
        Team teamB;
        if (roundLevel == 0){
            teamA = atomicTeamPicker.pickTeam();
            teamB = atomicTeamPicker.pickTeam();
        } else {
            teamA = playRound(atomicTeamPicker, roundLevel - 1);
            teamB = playRound(atomicTeamPicker, roundLevel - 1);
        }

        System.out.println("Round " + roundLevel);
        return playMatch(teamA, teamB);
    }

    private static Team playMatch(Team teamA, Team teamB) {
        int teamAScore = RANDOM.nextInt(5);
        int teamBScore = RANDOM.nextInt(5);
        while (teamAScore == teamBScore) {
            teamAScore = RANDOM.nextInt(5);
            teamBScore = RANDOM.nextInt(5);
        }
        System.out.printf("%s %d : %d %s\n",teamA, teamAScore, teamBScore, teamB);
        if (teamAScore > teamBScore) {
            return teamA;
        }
        return teamB;

    }

    public final static class AtomicTeamPicker {
        private final List<Team> teamList = new ArrayList<>(Arrays.asList(Team.values()));
        private final Iterator<Team> teamIterator;

        public AtomicTeamPicker() {
            Collections.shuffle(teamList);
            teamIterator = teamList.iterator();
        }

        public Team pickTeam() {
            if (teamIterator.hasNext()) {
              return teamIterator.next();
            }
            return null;
        }
    }

    public enum Team {
        ARGENTINA,
        FRANCE,
        BRAZIL,
        ENGLAND,
        BELGIUM,
        CROATIA,
        NETHERLANDS,
        ITALY,
        PORTUGAL,
        SPAIN,
        MOROCCO,
        MEXICO,
        SWITZERLAND,
        USA,
        GERMANY,
        URUGUAY,
        DENMARK,
        COLOMBIA,
        SENEGAL,
        JAPAN,
        IRAN,
        SWEDEN,
        SERBIA,
        POLAND,
        WALES,
        AUSTRALIA,
        TUNISIA,
        ECUADOR,
        KOREA_REPUBLIC,
        AUSTRIA,
        PERU,
        CZECH_REPUBLIC,
        COSTA_RICA,
        NORWAY,
        HUNGARY,
        RUSSIA,
        EGYPT,
        ALGERIA,
        CANADA,
        SCOTLAND,
        TURKEY,
        SLOVAKIA,
        CAMEROON,
        NIGERIA,
        COTE_DIVOIRE,
        GREECE,
        PARAGUAY,
        REPUBLIC_OF_IRELAND,
        SAUDI_ARABIA,
        ROMANIA,
        VENEZUELA,
        NORTHERN_IRELAND,
        GHANA,
        SLOVENIA,
        MALI,
        BURKINA_FASO,
        BOSNIA_AND_HERZEGOVINA,
        ICELAND,
        JAMAICA,
        FINLAND,
        PANAMA,
        UZBEKISTAN,
        QATAR,
        ISRAEL,
        SOUTH_AFRICA,
        IRAQ,
        NORTH_MACEDONIA,
        CONGO_DR,
        ALBANIA,
        OMAN,
        CAPE_VERDE,
        GABON,
        GEORGIA,
        HONDURAS,
        GUINEA,
        ZAMBIA,
        BOLIVIA,
        LIBYA,
        TAJIKISTAN,
        EL_SALVADOR,
        BENIN,
        KENYA,
        MADAGASCAR,
        SIERRA_LEONE
    }


}
