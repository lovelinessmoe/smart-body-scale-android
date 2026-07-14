package com.example.dianzicheng.data.local

import androidx.room.TypeConverter
import com.example.dianzicheng.domain.Sex

class Converters {
    @TypeConverter
    fun fromSex(sex: Sex): String {
        return sex.name
    }

    @TypeConverter
    fun toSex(value: String): Sex {
        return Sex.valueOf(value)
    }
}
