package com.example.pomostudy.data.repository

import com.example.pomostudy.data.local.dao.TagDao
import com.example.pomostudy.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/** 标签数据仓库：统一管理标签的增删改查。 */
class TagRepository(private val tagDao: TagDao) {

    val tags: Flow<List<TagEntity>> = tagDao.observeAll()

    suspend fun add(name: String, color: Long): Long = tagDao.insert(TagEntity(name = name, color = color))

    suspend fun update(tag: TagEntity) = tagDao.update(tag)

    /**
     * 删除标签。
     * 依据外键 ON DELETE SET NULL，关联的学习记录与待办的 tagId 会被数据库自动置空。
     */
    suspend fun delete(id: Long) = tagDao.deleteById(id)

    suspend fun getById(id: Long): TagEntity? = tagDao.getById(id)
}
