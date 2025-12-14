package com.yogakotlinpipeline.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yogakotlinpipeline.app.databinding.ItemPoseBinding

class PoseAdapter(
    private val onPoseClick: (ExploreFragment.Pose) -> Unit
) : ListAdapter<ExploreFragment.Pose, PoseAdapter.PoseViewHolder>(PoseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoseViewHolder {
        val binding = ItemPoseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PoseViewHolder(binding, onPoseClick)
    }

    override fun onBindViewHolder(holder: PoseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PoseViewHolder(
        private val binding: ItemPoseBinding,
        private val onPoseClick: (ExploreFragment.Pose) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pose: ExploreFragment.Pose) {
            binding.tvPoseName.text = pose.displayName
            binding.tvPoseDescription.text = pose.description
            
            // Set difficulty level with color coding
            binding.tvPoseDifficulty.text = pose.difficulty
            binding.tvPoseDifficulty.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    when (pose.difficulty.lowercase()) {
                        "beginner" -> android.R.color.holo_green_dark
                        "intermediate" -> android.R.color.holo_orange_dark
                        "advanced" -> android.R.color.holo_red_dark
                        else -> android.R.color.darker_gray
                    }
                )
            )
            
            // Set Sanskrit name if available
            binding.tvSanskritName.text = pose.name
            
            binding.root.setOnClickListener {
                android.util.Log.d("PoseAdapter", "=== Pose clicked: ${pose.name} ===")
                onPoseClick(pose)
            }
        }
    }

    private class PoseDiffCallback : DiffUtil.ItemCallback<ExploreFragment.Pose>() {
        override fun areItemsTheSame(oldItem: ExploreFragment.Pose, newItem: ExploreFragment.Pose): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: ExploreFragment.Pose, newItem: ExploreFragment.Pose): Boolean {
            return oldItem == newItem
        }
    }
}
