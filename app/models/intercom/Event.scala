package models.intercom

import java.sql.Timestamp

case class Event(
                name: String,
                createdAt: Timestamp,
                userEmail: String
                // Add more if necessary
                )

object Event {
  // Json writes
}