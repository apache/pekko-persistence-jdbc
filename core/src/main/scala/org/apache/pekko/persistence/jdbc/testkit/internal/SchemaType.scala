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

package org.apache.pekko.persistence.jdbc.testkit.internal

import org.apache.pekko.annotation.InternalApi

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] sealed trait SchemaType

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] case object Postgres extends SchemaType

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] case object H2 extends SchemaType

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] case object MySQL extends SchemaType

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] case object Oracle extends SchemaType

/**
 * INTERNAL API
 */
@InternalApi private[jdbc] case object SqlServer extends SchemaType
