/**
 * Copyright (c) 2016-2017 Zerocracy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to read
 * the Software only. Permissions is hereby NOT GRANTED to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.zerocracy.stk.slack;

import com.jcabi.aspects.Tv;
import com.jcabi.log.Logger;
import com.jcabi.xml.XML;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.zerocracy.jstk.Project;
import com.zerocracy.jstk.Stakeholder;
import com.zerocracy.pm.ClaimIn;
import java.util.Map;

/**
 * Notify in Slack.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.8
 */
public final class StkNotify implements Stakeholder {

    /**
     * Sessions.
     */
    private final Map<String, SlackSession> sessions;

    /**
     * Ctor.
     * @param map Map of sessions
     */
    public StkNotify(final Map<String, SlackSession> map) {
        this.sessions = map;
    }

    @Override
    public void process(final Project project, final XML xml) {
        final ClaimIn claim = new ClaimIn(xml);
        final String[] parts = claim.token().split(";");
        final SlackSession session = this.session(parts[1]);
        final String message = claim.param("message").replaceAll(
            "\\[([^]]+)]\\(([^)]+)\\)", "<$2|$1>"
        );
        if (parts.length > Tv.THREE && "direct".equals(parts[Tv.THREE])) {
            session.sendMessage(
                session.openDirectMessageChannel(
                    session.findUserByUserName(parts[2])
                ).getReply().getSlackChannel(),
                message
            );
        } else {
            final SlackChannel channel = session.findChannelById(parts[1]);
            if (parts.length > 2) {
                session.sendMessage(
                    channel,
                    String.format("@%s %s", parts[2], message)
                );
                Logger.info(
                    this, "@%s posted %d chars to @%s at %s/%s",
                    session.sessionPersona().getUserName(),
                    message.length(),
                    parts[2],
                    channel.getName(), channel.getId()
                );
            } else {
                session.sendMessage(channel, message);
                Logger.info(
                    this, "@%s posted %d chars at %s/%s",
                    session.sessionPersona().getUserName(),
                    message.length(),
                    channel.getName(), channel.getId()
                );
            }
        }
    }

    /**
     * Find session by channel ID.
     * @param channel Channel ID
     * @return Session
     */
    public SlackSession session(final String channel) {
        for (final SlackSession session : this.sessions.values()) {
            if (session.findChannelById(channel) != null) {
                return session;
            }
        }
        throw new IllegalArgumentException(
            String.format(
                "Can't find Slack session for channel \"%s\"",
                channel
            )
        );
    }

}
