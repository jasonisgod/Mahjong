package com.github.blovemaple.mj.local;

import static com.github.blovemaple.mj.action.standard.PlayerActionTypes.*;
import static com.github.blovemaple.mj.utils.MyUtils.*;
import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.blovemaple.mj.action.Action;
import com.github.blovemaple.mj.action.PlayerAction;
import com.github.blovemaple.mj.action.PlayerActionType;
import com.github.blovemaple.mj.cli.CliGameView;
import com.github.blovemaple.mj.game.GameContextPlayerView;
import com.github.blovemaple.mj.object.Player;
import com.github.blovemaple.mj.object.Tile;

/**
 * @author blovemaple <blovemaple2010(at)gmail.com>
 */
public abstract class AbstractBot implements Player {
	private static final Logger logger = Logger.getLogger(AbstractBot.class.getSimpleName());

	private String name;
	private int minThinkingMs = 1000, maxThinkingMs = 3000;

	private long costSum;
	private int invokeCount;

	public AbstractBot(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setThinkingTime(int min, int max) {
		if (min > max)
			throw new IllegalArgumentException("Invalid thinking time: [" + min + "," + max + "]");
		this.minThinkingMs = min;
		this.maxThinkingMs = max;
	}

	public void resetCostStat() {
		costSum = 0;
		invokeCount = 0;
	}

	public long getCostSum() {
		return costSum;
	}

	public int getInvokeCount() {
		return invokeCount;
	}

	@Override
	public PlayerAction chooseAction(GameContextPlayerView contextView, Set<PlayerActionType> actionTypes)
			throws InterruptedException {
		long startTime = System.nanoTime();

		logger.info(() -> "BOT Alive tiles: " + aliveTilesStr(contextView));
		PlayerAction action = chooseAction0(contextView, actionTypes);
		logger.info(() -> "BOT Chosed action:" + action);

		long endTime = System.nanoTime();
		long nanoCost = endTime - startTime;
		long delayMillis = minThinkingMs - TimeUnit.MILLISECONDS.convert(nanoCost, TimeUnit.NANOSECONDS);
		TimeUnit.MILLISECONDS.sleep(delayMillis);

		return action;
	}

	private PlayerAction chooseAction0(GameContextPlayerView contextView, Set<PlayerActionType> actionTypes)
			throws InterruptedException {
		// ????????????????????????
		if (actionTypes.contains(WIN))
			return new PlayerAction(contextView.getMyLocation(), WIN);

		// ??????????????????????????????
		if (actionTypes.contains(DRAW_BOTTOM))
			return new PlayerAction(contextView.getMyLocation(), DRAW_BOTTOM);

		// ??????????????????????????????
		if (actionTypes.contains(BUHUA)) {
			Collection<Set<Tile>> buhuas = BUHUA.getLegalActionTiles(contextView);
			if (!buhuas.isEmpty()) {
				return new PlayerAction(contextView.getMyLocation(), BUHUA, buhuas.iterator().next());
			}
		}

		// ???????????????/???/???/??????????????????
		List<PlayerAction> cpgdActions = Stream
				.concat(cpgdActions(contextView, actionTypes), passAction(contextView)).collect(toList());
		PlayerAction action = cpgdActions.isEmpty() ? null
				: cpgdActions.size() == 1 ? cpgdActions.get(0)
						: chooseCpgdActionWithTimer(contextView, actionTypes, cpgdActions);
		if (action != null)
			return action;

		// ??????????????????????????????
		if (actionTypes.contains(DRAW))
			return new PlayerAction(contextView.getMyLocation(), DRAW);

		// ???????????????????????????
		return null;
	}

	private String aliveTilesStr(GameContextPlayerView contextView) {
		StringBuilder aliveTilesStr = new StringBuilder();
		Tile justDrawed = contextView.getMyInfo().getLastDrawedTile();
		CliGameView.appendAliveTiles(aliveTilesStr, contextView.getMyInfo().getAliveTiles(), null,
				justDrawed != null ? Set.of(contextView.getMyInfo().getLastDrawedTile()) : null);
		return aliveTilesStr.toString();
	}

	private Stream<PlayerAction> cpgdActions(GameContextPlayerView contextView, Set<PlayerActionType> actionTypes) {
		return
		// ???/???/???/??????????????????
		Stream.of(CHI, PENG, ZHIGANG, BUGANG, ANGANG, DISCARD, DISCARD_WITH_TING)
				// ?????????actionTypes??????
				.filter(actionTypes::contains)
				// ??????????????????????????????????????????
				.flatMap(actionType -> actionType
						// ????????????tiles
						.getLegalActionTiles(contextView).stream()
						// ???????????????
						.filter(distinctorBy(
								tiles -> tiles.stream().map(Tile::type).sorted().collect(Collectors.toList())))
						// ??????Action
						.map(tiles -> new PlayerAction(contextView.getMyLocation(), actionType, tiles)));
	}

	private Stream<PlayerAction> passAction(GameContextPlayerView contextView) {
		if (contextView.getMyInfo().getAliveTiles().size() % 3 == 1)
			return Stream.of((PlayerAction) null);
		else
			return Stream.empty();
	}

	private PlayerAction chooseCpgdActionWithTimer(GameContextPlayerView contextView, Set<PlayerActionType> actionTypes,
			List<PlayerAction> actions) throws InterruptedException {
		long startTime = System.nanoTime();
		try {
			return chooseCpgdAction(contextView, actionTypes, actions);
		} finally {
			long endTime = System.nanoTime();
			long nanoCost = endTime - startTime;
			costSum += nanoCost;
			invokeCount++;
			logger.info("BOT Time cost(millis): " + Math.round(nanoCost / 1_000_000D));

			// ?????????????????????????????????????????????
			int targetThinkingTime = minThinkingMs + (int) (Math.random() * (maxThinkingMs - minThinkingMs));
			long delayMillis = targetThinkingTime - TimeUnit.MILLISECONDS.convert(nanoCost, TimeUnit.NANOSECONDS);
			TimeUnit.MILLISECONDS.sleep(delayMillis);
		}
	}

	protected abstract PlayerAction chooseCpgdAction(GameContextPlayerView contextView,
			Set<PlayerActionType> actionTypes, List<PlayerAction> actions) throws InterruptedException;

	@Override
	public PlayerAction chooseAction(GameContextPlayerView contextView, Set<PlayerActionType> actionTypes,
			PlayerAction illegalAction) throws InterruptedException {
		logger.severe("Selected illegal action: " + illegalAction);
		return null;
	}

	@Override
	public void actionDone(GameContextPlayerView contextView, Action action) {
	}

	@Override
	public void timeLimit(GameContextPlayerView contextView, Integer secondsToGo) {
	}

}
