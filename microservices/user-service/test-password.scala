import org.mindrot.jbcrypt.BCrypt

object TestPasswordHash {
  def main(args: Array[String]): Unit = {
    val plainPassword = "12345678"
    val hashedFromSQL = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

    println(s"Testing password: $plainPassword")
    println(s"Hash from SQL: $hashedFromSQL")

    val isValid = BCrypt.checkpw(plainPassword, hashedFromSQL)
    println(s"Password matches: $isValid")

    // Generate new hash for comparison
    val newHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10))
    println(s"\nNew hash generated: $newHash")

    // Verify the new hash
    val newHashValid = BCrypt.checkpw(plainPassword, newHash)
    println(s"New hash valid: $newHashValid")
  }
}
