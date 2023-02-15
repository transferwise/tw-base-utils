package com.transferwise.common.baseutils.clock

import spock.lang.Specification

import java.time.Instant

class TestClockSpec extends Specification {
    def "plus can be used from groovy"() {
        given:
        def clock = new TestClock(Instant.parse('2021-11-04T00:00:00Z'))

        when:
        clock + 'P1D'

        then:
        clock.instant().toString() == '2021-11-05T00:00:00Z'
    }
}
