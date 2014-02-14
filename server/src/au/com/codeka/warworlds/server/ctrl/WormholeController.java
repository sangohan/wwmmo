package au.com.codeka.warworlds.server.ctrl;

import com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.model.Star;

public class WormholeController {

    public void tuneWormhole(Star srcWormhole, Star destWormhole) throws RequestException {
        // you can only tune wormholes to another wormhole in your alliance
        if (srcWormhole.getWormholeExtra().getEmpireID() != destWormhole.getWormholeExtra().getEmpireID()) {
            int srcEmpireID = srcWormhole.getWormholeExtra().getEmpireID();
            int destEmpireID = destWormhole.getWormholeExtra().getEmpireID();
            if (!new AllianceController().isSameAlliance(srcEmpireID, destEmpireID)) {
                throw new RequestException(400);
            }
        }

        srcWormhole.getWormholeExtra().tuneTo(destWormhole.getID());
        try {
            new StarController().update(srcWormhole);
        } catch (MySQLTransactionRollbackException e) {
            throw new RequestException(e);
        }
    }
}
