package com.github.blovemaple.mj.local;

import static com.github.blovemaple.mj.object.PlayerLocation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

import com.github.blovemaple.mj.game.MahjongGame;
import com.github.blovemaple.mj.local.bazbot.BazBot;
import com.github.blovemaple.mj.object.MahjongTable;
import com.github.blovemaple.mj.object.Player;
import com.github.blovemaple.mj.rule.GameStrategy;
import com.github.blovemaple.mj.rule.TimeLimitStrategy;
import com.github.blovemaple.mj.rule.simple.SimpleGameStrategy;
import com.github.blovemaple.mj.rule.simple.SimpleTimeLimitStrategy;

/**
 * 本地游戏。
 * 
 * @author blovemaple <blovemaple2010(at)gmail.com>
 */
public class LocalGame {
	private GameStrategy gameStrategy = new SimpleGameStrategy();
	private TimeLimitStrategy timeStrategy = new SimpleTimeLimitStrategy(5, TimeUnit.SECONDS); //TimeLimitStrategy.NO_LIMIT;
	private Class<? extends Player> botPlayerClass = BazBot.class;

	private Player localPlayer;
	private Supplier<Boolean> newGameChecker;

	/**
	 * 新建一个实例。
	 * 
	 * @param localPlayer
	 *            本地玩家
	 * @param newGameChecker
	 *            一局结束后决定是否开始新的一局的函数
	 */
	public LocalGame(Player localPlayer, Supplier<Boolean> newGameChecker) {
		this.localPlayer = localPlayer;
		this.newGameChecker = newGameChecker;
	}

	public void play() throws InterruptedException {
		try {
			MahjongTable table = new MahjongTable();
			table.init();
			table.setPlayer(EAST, localPlayer);
			table.setPlayer(SOUTH, botPlayerClass.getConstructor().newInstance());
			table.setPlayer(WEST, botPlayerClass.getConstructor().newInstance());
			table.setPlayer(NORTH, botPlayerClass.getConstructor().newInstance());

			MahjongGame game = new MahjongGame(gameStrategy, timeStrategy);
			while (newGameChecker.get()) {
				game.play(table);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
}
