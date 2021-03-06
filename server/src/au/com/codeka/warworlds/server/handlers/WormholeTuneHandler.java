package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.WormholeController;
import au.com.codeka.warworlds.server.model.Alliance;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Star;

public class WormholeTuneHandler extends RequestHandler {
    @Override
    protected void post() throws RequestException {
        int starID = Integer.parseInt(getUrlParameter("star_id"));

        Star srcWormhole = new StarController().getStar(starID);
        if (srcWormhole.getStarType().getType() != Star.Type.Wormhole) {
            throw new RequestException(404);
        }

        Messages.WormholeTuneRequest pb = getRequestBody(Messages.WormholeTuneRequest.class);
        if (pb.getSrcStarId() != starID) {
            throw new RequestException(404);
        }

        Star destWormhole = new StarController().getStar(pb.getDestStarId());
        if (destWormhole.getStarType().getType() != Star.Type.Wormhole) {
            throw new RequestException(400);
        }

        Star.WormholeExtra srcWormholeExtra = srcWormhole.getWormholeExtra();
        Star.WormholeExtra destWormholeExtra = destWormhole.getWormholeExtra();

        Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
        if (srcWormholeExtra.getEmpireID() != empire.getID()) {
            Empire ownerEmpire = new EmpireController().getEmpire(srcWormholeExtra.getEmpireID());
            if (ownerEmpire.getAlliance() == null ||
                    empire.getAlliance() == null ||
                    ((Alliance) ownerEmpire.getAlliance()).getID() != ((Alliance) empire.getAlliance()).getID()) {
                throw new RequestException(400, "You do not have control of this wormhole and cannot tune it.");
            }
        }

        if (destWormholeExtra.getEmpireID() != empire.getID()) {
            Empire ownerEmpire = new EmpireController().getEmpire(destWormholeExtra.getEmpireID());
            if (ownerEmpire.getAlliance() == null ||
                    empire.getAlliance() == null ||
                    ((Alliance) ownerEmpire.getAlliance()).getID() != ((Alliance) empire.getAlliance()).getID()) {
                throw new RequestException(400, "You do not have control of the destination wormhole and cannot tune it.");
            }
        }

        new WormholeController().tuneWormhole(srcWormhole, destWormhole);

        srcWormhole = new StarController().getStar(srcWormhole.getID());
        Messages.Star.Builder star_pb = Messages.Star.newBuilder();
        srcWormhole.toProtocolBuffer(star_pb);
        setResponseBody(star_pb.build());
    }
}
