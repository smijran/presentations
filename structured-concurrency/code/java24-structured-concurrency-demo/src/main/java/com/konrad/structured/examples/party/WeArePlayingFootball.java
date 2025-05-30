package com.konrad.structured.examples.party;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.StructuredTaskScope;

import static com.konrad.structured.examples.party.DemoThreadFactory.VT_THREAD_FACTORY;

public class WeArePlayingFootball {

    private static final Logger log = LogManager.getLogger(WeArePlayingFootball.class);

    void main() throws InterruptedException {
        final AtomicTeamPicker atomicTeamPicker = new AtomicTeamPicker();
        playRound(atomicTeamPicker, 5);
    }

    private static Team playRound(AtomicTeamPicker atomicTeamPicker, int roundLevel) throws InterruptedException {
        if (roundLevel == 0) {
            Team teamA = atomicTeamPicker.pickTeam();
            Team teamB = atomicTeamPicker.pickTeam();
            return playMatch(roundLevel, teamA, teamB);
        }

        try (var playSubFinals = StructuredTaskScope.open()) {
            StructuredTaskScope.Subtask<Team> teamA =
                    playSubFinals.fork(() -> playRound(atomicTeamPicker, roundLevel - 1));
            StructuredTaskScope.Subtask<Team> teamB =
                    playSubFinals.fork(() -> playRound(atomicTeamPicker, roundLevel - 1));

            playSubFinals.join();

            return playMatch(roundLevel, teamA.get(), teamB.get());
        }
    }

    private static Team playMatch(int round, Team teamA, Team teamB) throws InterruptedException {
        final Random random = new Random();
        final int teamAScore = random.nextInt(5);
        final int teamBScore = random.nextInt(5);
        Team result = teamB;
        if (teamAScore > teamBScore) {
            result = teamA;
        } else if (teamAScore == teamBScore) {
            result = random.nextBoolean() ? teamA : teamB;
        }
        Thread.sleep(random.nextInt(7000));
        log.info("Round {} - {} {} : {} {} - Wins: {}", round, teamA, teamAScore, teamBScore, teamB, result);
        return result;

    }

}

final class AtomicTeamPicker {
    private final Iterator<Team> teamIterator;

    public AtomicTeamPicker() {
        List<Team> teamList = new ArrayList<>(Arrays.asList(Team.values()));
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

enum Team {
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