package chkNetwork.Server;

import chkMVC.chModel.Checkers.GameEventListener;
import chkMVC.chModel.Checkers.PIECE_TEAM;
import chkMVC.chModel.Checkers.Pieces.AbstractPiece;
import chkMVC.chModel.Checkers.Position;
import chkNetwork.SERVER_RESPONSE_CODES;

import java.util.List;
import java.util.Map;

public class ServerGameListener implements GameEventListener {

    private final CheckerNetworkService networkService;

    public ServerGameListener (CheckerNetworkService networkService) {
        this.networkService = networkService;
    }


    @Override
    public void onGameComplete (PIECE_TEAM currentTurn) {

        System.out.println("Game Completed AND WON by " + currentTurn.toString());

        ServerResponse gameFinishMessage = ServerResponse.create(SERVER_RESPONSE_CODES.GAME_END, List.of(currentTurn.toString()));

        networkService.broadcastMessage(gameFinishMessage, null);


        networkService.resetAuthGameModel();
    }

    @Override
    public void onMoveMade (Position from, Position to) {
        System.out.println("Server Listener: move confirmed from " + from + " to " + to);


        List<String> movePayload = List.of(from.toString(), to.toString());

        ServerResponse moveUpdate = ServerResponse.create(SERVER_RESPONSE_CODES.MOVE_PIECE, movePayload);

        networkService.broadcastMessage(moveUpdate, null);

        PIECE_TEAM nextTurn = networkService.getAuthGameModel().getCurrentTurn();

        ServerResponse roundUpdate = ServerResponse.create(SERVER_RESPONSE_CODES.ROUND_UPDATE, List.of(nextTurn.toString()));

        networkService.broadcastMessage(roundUpdate, null);

    }

    @Override
    public void onBoardUpdate (Map<Position, AbstractPiece> boardState) {

    }

    @Override
    public void onTurnChange (PIECE_TEAM currentTurn) {
        networkService.broadcastMessage(ServerResponse.create(SERVER_RESPONSE_CODES.ROUND_UPDATE, List.of(currentTurn.toString())), null);

    }

    @Override
    public void onPieceRemoved (Position position) {
        System.out.println("Piece Removed from: " + position.toString());

        List<String> payload = List.of(position.toString());

        ServerResponse removeUpdate = ServerResponse.create(SERVER_RESPONSE_CODES.REMOVE_PIECE, payload);

        networkService.broadcastMessage(removeUpdate, null);

    }
}
