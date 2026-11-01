package rw.vikingfc.club.competition;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.vikingfc.club.common.LabelText;
import rw.vikingfc.club.common.exception.NotFoundException;
import rw.vikingfc.club.match.Opponent;
import rw.vikingfc.club.match.OpponentRepository;
import rw.vikingfc.club.settings.SettingsService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository teamRepository;
    private final OpponentRepository opponentRepository;
    private final SettingsService settingsService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return competitionRepository.findBySeasonIdAndArchivedFalseOrderByNameAsc(settingsService.currentSeason().getId()).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public Map<String, Object> create(String name, CompetitionType type, Boolean official, Boolean countsForTable, String color, Integer squadSize) {
        Competition competition = new Competition();
        competition.setSeason(settingsService.currentSeason());
        competition.setName(name);
        competition.setType(type == null ? CompetitionType.OTHER : type);
        boolean friendly = competition.getType() == CompetitionType.FRIENDLY;
        competition.setOfficial(friendly ? false : official == null || official);
        competition.setCountsForTable(!friendly && Boolean.TRUE.equals(countsForTable) || competition.getType() == CompetitionType.LEAGUE && countsForTable == null);
        if (competition.getType() != CompetitionType.LEAGUE && !Boolean.TRUE.equals(countsForTable)) {
            competition.setCountsForTable(false);
        }
        competition.setColor(color == null || color.isBlank() ? (friendly ? "#5c6570" : "#a3283a") : color);
        if (squadSize != null) {
            competition.setSquadSize(squadSize);
        }
        applyNames(competition, name, null, null, null);
        return view(competitionRepository.save(competition));
    }

    @Transactional
    public Map<String, Object> create(String name, CompetitionType type, Boolean official, Boolean countsForTable, String color, Integer squadSize,
                                      String sourceLang, String nameFr, String nameRw) {
        Map<String, Object> created = create(name, type, official, countsForTable, color, squadSize);
        Competition competition = load(((Number) created.get("id")).longValue());
        applyNames(competition, name, sourceLang, nameFr, nameRw);
        return view(competition);
    }

    @Transactional
    public Map<String, Object> update(Long id, String name, CompetitionType type, Boolean official, Boolean countsForTable, String color, Boolean archived) {
        Competition competition = load(id);
        if (name != null && !name.isBlank()) {
            competition.setName(name);
        }
        if (type != null) {
            competition.setType(type);
        }
        if (official != null) {
            competition.setOfficial(official);
        }
        if (countsForTable != null) {
            competition.setCountsForTable(countsForTable && competition.getType() == CompetitionType.LEAGUE);
        }
        if (competition.getType() == CompetitionType.FRIENDLY) {
            competition.setOfficial(false);
            competition.setCountsForTable(false);
        }
        if (color != null && !color.isBlank()) {
            competition.setColor(color);
        }
        if (archived != null) {
            competition.setArchived(archived);
        }
        return view(competition);
    }

    public void applyNames(Competition competition, String name, String sourceLang, String nameFr, String nameRw) {
        if (name != null && !name.isBlank()) {
            competition.setName(name.trim());
        }
        if (sourceLang != null && !sourceLang.isBlank()) {
            competition.setSourceLang(sourceLang.trim().toLowerCase());
        } else if (competition.getSourceLang() == null) {
            competition.setSourceLang("en");
        }
        if (nameFr != null) {
            competition.setNameFr(nameFr.trim());
        }
        if (nameRw != null) {
            competition.setNameRw(nameRw.trim());
        }
    }

    @Transactional
    public Map<String, Object> addTeam(Long competitionId, Long opponentId, String name, String shortName, String city) {
        Competition competition = load(competitionId);
        Opponent opponent = opponentId == null ? null : opponentRepository.findById(opponentId).orElse(null);
        if (opponent == null) {
            opponent = new Opponent();
            opponent.setName(name);
            opponent.setShortName(shortName);
            opponent.setCity(city);
            opponent = opponentRepository.save(opponent);
        }
        if (teamRepository.findByCompetitionIdAndOpponentId(competitionId, opponent.getId()).isEmpty()) {
            CompetitionTeam link = new CompetitionTeam();
            link.setCompetition(competition);
            link.setOpponent(opponent);
            teamRepository.save(link);
        }
        return view(competition);
    }

    @Transactional
    public void removeTeam(Long competitionId, Long opponentId) {
        teamRepository.deleteByCompetitionIdAndOpponentId(competitionId, opponentId);
    }

    @Transactional(readOnly = true)
    public Competition load(Long id) {
        return competitionRepository.findById(id).orElseThrow(() -> NotFoundException.of("Competition", id));
    }

    public Map<String, Object> view(Competition competition) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", competition.getId());
        String source = competition.getSourceLang() == null ? "en" : competition.getSourceLang();
        body.put("name", competition.getName());
        body.put("sourceLang", source);
        body.put("nameFr", competition.getNameFr());
        body.put("nameRw", competition.getNameRw());
        body.put("label", java.util.Map.of(
                "en", LabelText.pick("en", source, competition.getName(), competition.getName(), competition.getNameFr(), competition.getNameRw()),
                "fr", LabelText.pick("fr", source, competition.getName(), competition.getName(), competition.getNameFr(), competition.getNameRw()),
                "rw", LabelText.pick("rw", source, competition.getName(), competition.getName(), competition.getNameFr(), competition.getNameRw())
        ));
        body.put("type", competition.getType());
        body.put("official", competition.isOfficial());
        body.put("countsForTable", competition.isCountsForTable());
        body.put("color", competition.getColor());
        body.put("logoUrl", competition.getLogoUrl());
        body.put("squadSize", competition.getSquadSize());
        body.put("maxBench", competition.getMaxBench());
        body.put("minGoalkeepers", competition.getMinGoalkeepers());
        body.put("winPoints", competition.getWinPoints());
        body.put("drawPoints", competition.getDrawPoints());
        body.put("lossPoints", competition.getLossPoints());
        body.put("archived", competition.isArchived());
        body.put("teams", teamRepository.findByCompetitionId(competition.getId()).stream().map(link -> {
            Map<String, Object> team = new LinkedHashMap<>();
            team.put("id", link.getOpponent().getId());
            team.put("name", link.getOpponent().getName());
            team.put("shortName", link.getOpponent().getShortName());
            team.put("city", link.getOpponent().getCity());
            team.put("logoUrl", link.getOpponent().getLogoUrl());
            return team;
        }).toList());
        return body;
    }
}
