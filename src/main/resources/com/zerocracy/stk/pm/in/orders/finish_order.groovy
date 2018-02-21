/**
 * Copyright (c) 2016-2018 Zerocracy
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
package com.zerocracy.stk.pm.in.orders

import com.jcabi.xml.XML
import com.zerocracy.Par
import com.zerocracy.Project
import com.zerocracy.cash.Cash
import com.zerocracy.farm.Assume
import com.zerocracy.pm.ClaimIn
import com.zerocracy.pm.ClaimOut
import com.zerocracy.pm.cost.Boosts
import com.zerocracy.pm.cost.Estimates
import com.zerocracy.pm.cost.Rates
import com.zerocracy.pm.in.Orders
import java.util.concurrent.TimeUnit

def exec(Project project, XML xml) {
  new Assume(project, xml).notPmo()
  new Assume(project, xml).type('Finish order')
  ClaimIn claim = new ClaimIn(xml)
  String job = claim.param('job')
  Orders orders = new Orders(project).bootstrap()
  long minutes = (System.currentTimeMillis() - orders.startTime(job).time) / TimeUnit.MINUTES.toMillis(1L)
  String login = orders.performer(job)
  Estimates estimates = new Estimates(project).bootstrap()
  String quality = claim.params().with {
    it.containsKey('quality') ? it['quality'] : 'acceptable'
  }
  if (quality == 'good' || quality == 'acceptable') {
    int extra = quality == 'good' ? 5 : 0
    ClaimOut out = claim.copy()
      .type('Make payment')
      .param('cause', claim.cid())
      .param('login', login)
      .param('reason', new Par('Order was finished, quality was "%s"').say(quality))
      .param('minutes', new Boosts(project).bootstrap().factor(job) * 15 + extra)
    if (estimates.exists(job)) {
      Cash price = estimates.get(job)
      Rates rates = new Rates(project).bootstrap()
      if (extra > 0 && rates.exists(login)) {
        price = price.add(rates.rate(login).mul(extra) / 60)
      }
      out = out.param('cash', price)
    }
    out.postTo(project)
  } else {
    claim.copy()
      .type('Notify job')
      .token("job;${job}")
      .param('job', job)
      .param('message', new Par('Quality is low, no payment, see §31').say())
      .postTo(project)
  }
  orders.resign(job)
  claim.copy()
    .type('Order was finished')
    .param('job', job)
    .param('login', login)
    .param('minutes', minutes)
    .postTo(project)
}
