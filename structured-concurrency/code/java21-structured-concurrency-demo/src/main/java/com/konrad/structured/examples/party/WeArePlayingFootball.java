package com.konrad;

import java.util.*;
import java.util.concurrent.StructuredTaskScope;

public class WeArePlayingFootball {


    private final static Random RANDOM = new Random();


    public static void main(String[] args) throws InterruptedException {
        final AtomicTeamPicker atomicTeamPicker = new AtomicTeamPicker();
        playRound(atomicTeamPicker, 4);
    }

    private static Team playRound(AtomicTeamPicker atomicTeamPicker, int roundLevel) throws InterruptedException {
        if (roundLevel == 0){
            Team teamA = atomicTeamPicker.pickTeam();
            Team teamB = atomicTeamPicker.pickTeam();
            return playMatch(roundLevel, teamA, teamB);
        }

        try (StructuredTaskScope<Team> playSubFinals  = new StructuredTaskScope<>()){
            StructuredTaskScope.Subtask<Team> teamA = playSubFinals.fork(() -> playRound(atomicTeamPicker, roundLevel - 1));
            StructuredTaskScope.Subtask<Team> teamB = playSubFinals.fork(() ->playRound(atomicTeamPicker, roundLevel - 1));

            playSubFinals.join();

            return playMatch(roundLevel, teamA.get(), teamB.get());
        } catch (InterruptedException e) {
            throw e;
        }
    }

    private static Team playMatch(int round, Team teamA, Team teamB) throws InterruptedException {
        final int teamAScore = RANDOM.nextInt(5);
        final int teamBScore = RANDOM.nextInt(5);
        Team result = teamB;
        if (teamAScore > teamBScore) {
            result = teamA;
        } else if (teamAScore == teamBScore) {
            result = RANDOM.nextBoolean() ? teamA : teamB;
        }
        Thread.sleep(RANDOM.nextInt(2000));
        System.out.printf("Round %d - %s %d : %d %s - Wins: %s \n", round, teamA, teamAScore, teamBScore, teamB, result);
        return result;

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
