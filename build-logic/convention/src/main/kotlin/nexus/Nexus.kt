package nexus

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.gradle.api.logging.Logger
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class Nexus(
  private val logger: Logger,
  username: String, password: String,
  private val groupId: String, baseUrl: String
) {

  private val service by lazy {
    val okHttpClient = OkHttpClient.Builder()
      .addInterceptor(NexusOkHttpInterceptor(username, password))
      .callTimeout(30, TimeUnit.SECONDS)
      .build()
    val moshi = Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
    val retrofit = Retrofit.Builder()
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .client(okHttpClient)
      .baseUrl(baseUrl)
      .build()

    retrofit.create(NexusService::class.java)
  }

  fun closeAndReleaseRepository() {
    val repositoryId = findAndCloseStagingRepository()
    releaseStagingRepository(repositoryId)
  }

  private fun findAndCloseStagingRepository(): String {
    val stagingRepository = findStagingRepository()
    val repositoryId = stagingRepository.repositoryId

    if (stagingRepository.type != "open") {
      throw IllegalArgumentException(
        "Repository $repositoryId is of type '${stagingRepository.type}' and not 'open'"
      )
    }

    logger.quiet("Closing repository: $repositoryId")
    val response = service.closeRepository(
      TransitionRepositoryInput(TransitionRepositoryInputData(listOf(repositoryId)))
    ).execute()
    if (!response.isSuccessful) {
      throw IOException("Cannot close repository: ${response.errorBody()?.string()}")
    }

    waitForClose(repositoryId)

    return repositoryId
  }

  private fun findStagingRepository(): Repository {
    val prefix = groupId.replace(".", "")
    val candidateRepositories = getProfileRepositories()
      ?.filter { it.repositoryId.startsWith(prefix) } ?: emptyList()

    if (candidateRepositories.isEmpty()) {
      throw IllegalArgumentException("No staging repository prefixed with \"$prefix\" found.")
    }

    if (candidateRepositories.size > 1) {
      throw IllegalArgumentException(
        "You have ${candidateRepositories.size} staging repositories, please login on " +
          "https://oss.sonatype.org and drop stale repositories. This script only works with one " +
          "active staging repository at a time."
      )
    }
    return candidateRepositories[0]
  }

  private fun getProfileRepositories(): List<Repository>? {
    val profileRepositoriesResponse = service.getProfileRepositories().execute()

    if (!profileRepositoriesResponse.isSuccessful) {
      throw IOException(
        "Cannot get profileRepositories: ${profileRepositoriesResponse.errorBody()?.string()}"
      )
    }

    return profileRepositoriesResponse.body()?.data
  }

  private fun waitForClose(repositoryId: String) {

    val startMillis = System.currentTimeMillis()

    val waitingChars = listOf(
      PROGRESS_1,
      PROGRESS_2,
      PROGRESS_3,
      PROGRESS_4,
      PROGRESS_5,
      PROGRESS_6,
      PROGRESS_7
    )
    var i = 0
    while (true) {
      if (System.currentTimeMillis() - startMillis > CLOSE_TIMEOUT_MILLIS) {
        throw IOException("Timeout waiting for repository close")
      }

      print("\r${waitingChars[i++ % waitingChars.size]} waiting for close...")
      System.out.flush()

      Thread.sleep(CLOSE_WAIT_INTERVAL_MILLIS)

      try {
        val repository = service.getRepository(repositoryId).execute().body()
        if (repository?.type == "closed" && !repository.transitioning) {
          break
        }
      } catch (e: IOException) {
        logger.error("Exception trying to get repository status: ${e.message}")
      }
    }
  }

  private fun releaseStagingRepository(repositoryId: String) {
    logger.quiet("Releasing repository: $repositoryId")
    val response = service.releaseRepository(
      TransitionRepositoryInput(
        TransitionRepositoryInputData(
          stagedRepositoryIds = listOf(repositoryId),
          autoDropAfterRelease = true
        )
      )
    ).execute()

    if (!response.isSuccessful) {
      throw IOException("Cannot release repository: ${response.errorBody()?.string()}")
    }

    logger.quiet("Repository $repositoryId released")
  }

  companion object {
    private const val PROGRESS_1 = "\u2839"
    private const val PROGRESS_2 = "\u2838"
    private const val PROGRESS_3 = "\u2834"
    private const val PROGRESS_4 = "\u2826"
    private const val PROGRESS_5 = "\u2807"
    private const val PROGRESS_6 = "\u280F"
    private const val PROGRESS_7 = "\u2819"

    private const val CLOSE_TIMEOUT_MILLIS = 15 * 60 * 1000L
    private const val CLOSE_WAIT_INTERVAL_MILLIS = 10_000L
  }
}
