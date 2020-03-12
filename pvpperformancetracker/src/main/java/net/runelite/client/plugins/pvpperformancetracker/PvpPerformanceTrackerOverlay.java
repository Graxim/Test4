/*
 * Copyright (c)  2020, Matsyir <https://github.com/matsyir>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.pvpperformancetracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import static net.runelite.api.MenuOpcode.RUNELITE_OVERLAY_CONFIG;
import net.runelite.client.ui.overlay.Overlay;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class PvpPerformanceTrackerOverlay extends Overlay
{
	private final PanelComponent panelComponent = new PanelComponent();
	private final PvpPerformanceTrackerPlugin plugin;
	private final PvpPerformanceTrackerConfig config;

	@Inject
	private PvpPerformanceTrackerOverlay(PvpPerformanceTrackerPlugin plugin, PvpPerformanceTrackerConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.BOTTOM_RIGHT);
		setPriority(OverlayPriority.LOW);
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "PvP Performance Tracker"));
		panelComponent.setPreferredSize(new Dimension(ComponentConstants.STANDARD_WIDTH, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		FightPerformance currentFight = plugin.getCurrentFight();
		if (currentFight == null || !config.showFightOverlay() ||
			(config.restrictToLms() && !plugin.isAtLMS()))
		{
			return null;
		}

		panelComponent.getChildren().clear();

		// Only display the title if it's enabled (pointless in my opinion, since you can just see
		// what the panel is displaying, but I can understand the preference of having overlays labelled)
		if (config.showOverlayTitle())
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("PvP Performance")
				.build());
		}

		// First line: Player's stats
		// Using simple overlay = left: RSN, right: success%
		// Not using simple overlay = left: 5 chars of RSN, right: stats string (successCount / totalCount success%)
		String playerName = plugin.getCurrentFight().getCompetitor().getName();
		panelComponent.getChildren().add(LineComponent.builder()
			.left(config.useSimpleOverlay() ?
				playerName :
				playerName.substring(0, Math.min(5, playerName.length())))
			.right(config.useSimpleOverlay() ?
				String.valueOf(Math.round(currentFight.getCompetitor().calculateSuccessPercentage())) + "%" :
				plugin.getCurrentFight().getCompetitor().getStats())
			.rightColor(plugin.getCurrentFight().playerWinning() ? Color.GREEN : Color.WHITE)
			.build());

		// Second line: Same as first line but opponent's stats.
		String opponentName = plugin.getCurrentFight().getOpponent().getName();
		panelComponent.getChildren().add(LineComponent.builder()
			.left(config.useSimpleOverlay() ?
				opponentName :
				opponentName.substring(0, Math.min(5, opponentName.length())))
			.right(config.useSimpleOverlay() ?
				String.valueOf(Math.round(currentFight.getOpponent().calculateSuccessPercentage())) + "%" :
				plugin.getCurrentFight().getOpponent().getStats())
			.rightColor(plugin.getCurrentFight().opponentWinning() ? Color.GREEN : Color.WHITE)
			.build());

		// Fix potential text overlap due to long RSN if displaying full RSN.
		if (config.useSimpleOverlay())
		{
			FontMetrics metrics = graphics.getFontMetrics();
			panelComponent.setPreferredSize(new Dimension(
				Math.max(ComponentConstants.STANDARD_WIDTH,
					Math.max(metrics.stringWidth(playerName), metrics.stringWidth(opponentName))
						+ metrics.stringWidth("100%") + 12),
				0));
		}
		else
		{
			panelComponent.setPreferredSize(new Dimension(ComponentConstants.STANDARD_WIDTH, 0));
		}

		return panelComponent.render(graphics);
	}
}