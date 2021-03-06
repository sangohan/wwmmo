package au.com.codeka.warworlds.server;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jregex.Matcher;
import jregex.Pattern;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.handlers.*;
import au.com.codeka.warworlds.server.handlers.admin.*;


public class RequestRouter extends AbstractHandler {
    private final Logger log = LoggerFactory.getLogger(RequestRouter.class);
    private static ArrayList<Route> sRoutes;

    {
        sRoutes = new ArrayList<Route>();
        sRoutes.add(new Route("login", LoginHandler.class));
        sRoutes.add(new Route("devices/({id}[0-9]*)$", DevicesHandler.class));
        sRoutes.add(new Route("devices$", DevicesHandler.class));
        sRoutes.add(new Route("hello/({device_id}[0-9]+)$", HelloHandler.class));
        sRoutes.add(new Route("chat$", ChatHandler.class));
        sRoutes.add(new Route("chat/({msg_id}[0-9]+)/abuse-reports", ChatAbuseReportHandler.class));
        sRoutes.add(new Route("chat/conversations$", ChatConversationsHandler.class));
        sRoutes.add(new Route("chat/conversations/({conversation_id}[0-9]+)/participants$", ChatConversationParticipantsHandler.class));
        sRoutes.add(new Route("chat/conversations/({conversation_id}[0-9]+)/participants/({empire_id}[0-9]+)$", ChatConversationParticipantHandler.class));
        sRoutes.add(new Route("empires$", EmpiresHandler.class));
        sRoutes.add(new Route("empires/search$", EmpiresSearchHandler.class));
        sRoutes.add(new Route("empires/({empire_id}[0-9]+)/stars$", EmpiresStarsHandler.class));
        sRoutes.add(new Route("empires/({empire_id}[0-9]+)/taxes$", EmpiresTaxesHandler.class));
        sRoutes.add(new Route("empires/({empire_id}[0-9]+)/cash-audit$", EmpiresCashAuditHandler.class));
        sRoutes.add(new Route("empires/({empire_id}[0-9]+)/display-name$", EmpiresDisplayNameHandler.class));
        sRoutes.add(new Route("empires/({empire_id}[0-9]+)/shield$", EmpiresShieldHandler.class));
        sRoutes.add(new Route("empires/({empire_id}[0-9]+)/reset$", EmpiresResetHandler.class));
        sRoutes.add(new Route("empires/({empire_id}[0-9]+)/ads$", EmpiresAdsHandler.class));
        sRoutes.add(new Route("buildqueue", BuildQueueHandler.class));
        sRoutes.add(new Route("sectors$", SectorsHandler.class));
        sRoutes.add(new Route("stars$", StarsHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)$", StarHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/simulate$", StarSimulateHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/build/({build_id}[0-9]+)/accelerate", BuildAccelerateHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/build/({build_id}[0-9]+)/stop", BuildStopHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/colonies$", ColoniesHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/colonies/({colony_id}[0-9]+)$", ColonyHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/colonies/({colony_id}[0-9]+)/attack$", ColonyAttackHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/combat-reports/({combat_report_id}[0-9]+)$", CombatReportHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/fleets/({fleet_id}[0-9]+)/orders$", FleetOrdersHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/fleets/({fleet_id}[0-9]+)$", FleetHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/scout-reports", ScoutReportsHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/sit-reports", SitReportsHandler.class));
        sRoutes.add(new Route("stars/({star_id}[0-9]+)/wormhole/tune", WormholeTuneHandler.class));
        sRoutes.add(new Route("alliances$", AlliancesHandler.class));
        sRoutes.add(new Route("alliances/({alliance_id}[0-9]+)$", AllianceHandler.class));
        sRoutes.add(new Route("alliances/({alliance_id}[0-9]+)/requests$", AllianceRequestsHandler.class));
        sRoutes.add(new Route("alliances/({alliance_id}[0-9]+)/requests/({request_id}[0-9]+)$", AllianceRequestHandler.class));
        sRoutes.add(new Route("alliances/({alliance_id}[0-9]+)/shield$", AllianceShieldHandler.class));
        sRoutes.add(new Route("alliances/({alliance_id}[0-9]+)/wormholes$", AllianceWormholeHandler.class));
        sRoutes.add(new Route("sit-reports/read", SitReportsReadHandler.class));
        sRoutes.add(new Route("sit-reports", SitReportsHandler.class));
        sRoutes.add(new Route("rankings/({year}[0-9]+)/({month}[0-9]+)$", RankingHistoryHandler.class));
        sRoutes.add(new Route("motd", MotdHandler.class));
        sRoutes.add(new Route("notifications$", NotificationHandler.class));
        sRoutes.add(new Route("error-reports$", ErrorReportsHandler.class));

        sRoutes.add(new Route("admin/?$", AdminDashboardHandler.class));
        sRoutes.add(new Route("admin/login$", AdminLoginHandler.class));
        sRoutes.add(new Route("admin/({path}actions/move-star)$", AdminActionsMoveStarHandler.class, "admin/"));
        sRoutes.add(new Route("admin/({path}actions/reset-empire)$", AdminActionsResetEmpireHandler.class, "admin/"));
        sRoutes.add(new Route("admin/alliance/({alliance_id}[0-9]+)/details$", AdminAllianceDetailsHandler.class));
        sRoutes.add(new Route("admin/chat$", AdminChatHandler.class));
        sRoutes.add(new Route("admin/chat/profanity$", AdminChatProfanityHandler.class));
        sRoutes.add(new Route("admin/chat/sinbin$", AdminChatSinbinHandler.class));
        sRoutes.add(new Route("admin/debug/purchases$", AdminDebugPurchasesHandler.class, "admin/"));
        sRoutes.add(new Route("admin/debug/error-reports$", AdminDebugErrorReportsHandler.class, "admin/"));
        sRoutes.add(new Route("admin/debug/retrace$", AdminDebugRetraceHandler.class, "admin/"));
        sRoutes.add(new Route("admin/empire/shields$", AdminEmpireShieldsHandler.class, "admin/"));
        sRoutes.add(new Route("admin/empire/alts$", AdminEmpireAltsHandler.class, "admin/"));
        sRoutes.add(new Route("admin/?({path}.*)", AdminGenericHandler.class, "admin/"));

        // TODO: move intel to a different handler
        sRoutes.add(new Route("intel/?({path}$)", AdminGenericHandler.class, "intel/"));
        sRoutes.add(new Route("intel/({path}.*)", StaticFileHandler.class, "intel/"));

        sRoutes.add(new Route("css/({path}.*)", StaticFileHandler.class, "css/"));
        sRoutes.add(new Route("js/({path}.*)", StaticFileHandler.class, "js/"));
        sRoutes.add(new Route("img/({path}.*)", StaticFileHandler.class, "img/"));
        sRoutes.add(new Route("({path}[^/]+)", StaticFileHandler.class, "/"));
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        for (Route route : sRoutes) {
            Matcher matcher = route.pattern.matcher(target);
            if (matcher.find()) {
                handle(matcher, route, request, response);
                baseRequest.setHandled(true);
                return;
            }
        }

        log.info(String.format("Could not find handler for URL: %s", target));
        response.setStatus(404);
    }

    private void handle(Matcher matcher, Route route, HttpServletRequest request,
                        HttpServletResponse response) {
        RequestHandler handler;
        try {
            handler = (RequestHandler) route.handlerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return; // TODO: error
        }

        handler.handle(matcher, route.extraOption, request, response);
    }

    private static class Route {
        public Pattern pattern;
        public Class<?> handlerClass;
        public String extraOption;

        public Route(String pattern, Class<?> handlerClass) {
            this(pattern, handlerClass, null);
        }
        public Route(String pattern, Class<?> handlerClass, String extraOption) {
            this("^/realms/({realm}[a-z]+)/"+pattern, false, handlerClass, extraOption);
        }
        public Route(String pattern, boolean dontAddRealm, Class<?> handlerClass, String extraOption) {
            this.pattern = new Pattern(pattern);
            this.handlerClass = handlerClass;
            this.extraOption = extraOption;
        }
    }
}
