package llm.slop.spirals

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import llm.slop.spirals.database.entities.MandalaTag
import llm.slop.spirals.database.entities.MandalaPatchEntity
import llm.slop.spirals.database.entities.MandalaSetEntity
import llm.slop.spirals.database.entities.MixerPatchEntity
import llm.slop.spirals.database.entities.ShowPatchEntity
import llm.slop.spirals.database.entities.RandomSetEntity
import llm.slop.spirals.database.daos.MandalaTagDao
import llm.slop.spirals.database.daos.MandalaPatchDao
import llm.slop.spirals.database.daos.MandalaSetDao
import llm.slop.spirals.database.daos.MixerPatchDao
import llm.slop.spirals.database.daos.ShowPatchDao
import llm.slop.spirals.database.daos.RandomSetDao

@Database(entities = [MandalaTag::class, MandalaPatchEntity::class, MandalaSetEntity::class, MixerPatchEntity::class, ShowPatchEntity::class, RandomSetEntity::class], version = 7, exportSchema = false)
abstract class MandalaDatabase : RoomDatabase() {
    abstract fun mandalaTagDao(): MandalaTagDao
    abstract fun mandalaPatchDao(): MandalaPatchDao
    abstract fun mandalaSetDao(): MandalaSetDao
    abstract fun mixerPatchDao(): MixerPatchDao
    abstract fun showPatchDao(): ShowPatchDao
    abstract fun randomSetDao(): RandomSetDao

    companion object {
        @Volatile
        private var INSTANCE: MandalaDatabase? = null

        fun getDatabase(context: Context): MandalaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MandalaDatabase::class.java,
                    "mandala_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
