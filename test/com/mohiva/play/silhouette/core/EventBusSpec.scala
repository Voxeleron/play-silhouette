/**
 * Copyright 2014 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mohiva.play.silhouette.core

import play.api.i18n.Lang
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import play.api.libs.concurrent.Akka
import akka.actor.{ Actor, Props }
import akka.testkit.TestProbe
import scala.concurrent.duration._
import org.specs2.specification.Scope

/**
 * Test case for the [[com.mohiva.play.silhouette.core.EventBus]] class.
 */
class EventBusSpec extends PlaySpecification {

  "The event bus" should {
    "handle an event" in new WithApplication with Context {
      val eventBus = new EventBus
      val listener = system.actorOf(Props(new Actor {
        def receive = {
          case e @ LoginEvent(_, _, _) => theProbe.ref ! e
        }
      }))

      eventBus.subscribe(listener, classOf[LoginEvent[TestIdentity]])
      eventBus.publish(loginEvent)

      theProbe.expectMsg(500 millis, loginEvent)
    }

    "handle multiple events" in new WithApplication with Context {
      val eventBus = new EventBus
      val listener = system.actorOf(Props(new Actor {
        def receive = {
          case e @ LoginEvent(_, _, _) => theProbe.ref ! e
          case e @ LogoutEvent(_, _, _) => theProbe.ref ! e
        }
      }))

      eventBus.subscribe(listener, classOf[LoginEvent[TestIdentity]])
      eventBus.subscribe(listener, classOf[LogoutEvent[TestIdentity]])
      eventBus.publish(loginEvent)
      eventBus.publish(logoutEvent)

      theProbe.expectMsg(500 millis, loginEvent)
      theProbe.expectMsg(500 millis, logoutEvent)
    }

    "differentiate between event classes" in new WithApplication with Context {
      val eventBus = new EventBus
      val listener = system.actorOf(Props(new Actor {
        def receive = {
          case e @ LoginEvent(_, _, _) => theProbe.ref ! e
        }
      }))

      eventBus.subscribe(listener, classOf[LogoutEvent[TestIdentity]])
      eventBus.publish(logoutEvent)

      theProbe.expectNoMsg(500 millis)
    }

    "not handle not subscribed events" in new WithApplication with Context {
      val eventBus = new EventBus
      val listener = system.actorOf(Props(new Actor {
        def receive = {
          case e @ LoginEvent(_, _, _) => theProbe.ref ! e
        }
      }))

      eventBus.publish(loginEvent)

      theProbe.expectNoMsg(500 millis)
    }

    "not handle events between different event buses" in new WithApplication with Context {
      val eventBus1 = new EventBus
      val eventBus2 = new EventBus

      val listener = system.actorOf(Props(new Actor {
        def receive = {
          case e @ LoginEvent(_, _, _) => theProbe.ref ! e
        }
      }))

      eventBus1.subscribe(listener, classOf[LoginEvent[TestIdentity]])
      eventBus2.publish(loginEvent)

      theProbe.expectNoMsg(500 millis)
    }
  }

  /**
   * An identity implementation.
   *
   * @param loginInfo The linked login info.
   */
  case class TestIdentity(loginInfo: LoginInfo) extends Identity

  /**
   * The context.
   */
  trait Context extends Scope {
    self: WithApplication =>

    /**
     * An identity implementation.
     */
    lazy val identity = TestIdentity(LoginInfo("test", "apollonia.vanova@watchmen.com"))

    /**
     * A fake request.
     */
    lazy val request = FakeRequest()

    /**
     * A language.
     */
    lazy val lang = Lang("en-US")

    /**
     * The Play actor system.
     */
    lazy implicit val system = Akka.system

    /**
     * The test probe.
     */
    lazy val theProbe = TestProbe()

    /**
     * Some events.
     */
    lazy val loginEvent = new LoginEvent[TestIdentity](identity, request, lang)
    lazy val logoutEvent = new LogoutEvent[TestIdentity](identity, request, lang)
  }
}
