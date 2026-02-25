package fyp.project.datingapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform