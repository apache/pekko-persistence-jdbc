/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc.configuration

import com.typesafe.config.ConfigFactory
import org.apache.pekko
import pekko.persistence.jdbc.SimpleSpec
import pekko.persistence.jdbc.util.ConfigOps.ConfigOperations

class ConfigOpsTest extends SimpleSpec {
  it should "parse field values to Options" in {
    val cfg = ConfigFactory.parseString("""
        | person {
        |   firstName = "foo"
        |   lastName = "bar"
        |   pet = ""
        |   car = " "
        | }
      """.stripMargin)

    cfg.asStringOption("person.firstName").get shouldBe "foo"
    cfg.asStringOption("person.lastName").get shouldBe "bar"
    cfg.asStringOption("person.pet") shouldBe None
    cfg.asStringOption("person.car") shouldBe None
    cfg.asStringOption("person.bike") shouldBe None
    cfg.asStringOption("person.bike") shouldBe None
  }
}
