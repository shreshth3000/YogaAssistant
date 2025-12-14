package com.yogakotlinpipeline.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yogakotlinpipeline.app.databinding.ItemYogaAsanaBinding

class YogaAsanaAdapter(
    // REMOVED: private val context: Context, - prevent Activity Context leak
    private var asanas: List<YogaAsana>,
    private val onAsanaClick: (YogaAsana) -> Unit
) : RecyclerView.Adapter<YogaAsanaAdapter.AsanaViewHolder>() {

    inner class AsanaViewHolder(private val binding: ItemYogaAsanaBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(asana: YogaAsana) {
            binding.tvDifficultyLevel.text = asana.difficultyLevel.name
            binding.tvAsanaName.text = asana.name
            binding.tvSanskritName.text = asana.sanskritName
            
            // CHANGED: Use binding.root.context instead of stored context
            val context = binding.root.context
            
            // Set difficulty level color
            val difficultyColor = when (asana.difficultyLevel) {
                DifficultyLevel.BEGINNER -> context.getColor(R.color.beginner_color)
                DifficultyLevel.INTERMEDIATE -> context.getColor(R.color.intermediate_color)
                DifficultyLevel.ADVANCED -> context.getColor(R.color.advanced_color)
            }
            binding.tvDifficultyLevel.setTextColor(difficultyColor)
            
            // CHANGED: Use binding.root.context for image loading
            val drawable = AssetImageHelper.loadImageFromAssets(context, asana.imageResource)
            if (drawable != null) {
                binding.ivAsanaImage.setImageDrawable(drawable)
            } else {
                // If asset loading fails, try to load from drawable resources
                try {
                    val resourceId = context.resources.getIdentifier(
                        asana.imageResource.replace(".png", "").replace(".xml", ""),
                        "drawable",
                        context.packageName
                    )
                    if (resourceId != 0) {
                        binding.ivAsanaImage.setImageResource(resourceId)
                    } else {
                        // Set a placeholder image
                        binding.ivAsanaImage.setImageResource(R.drawable.placeholder_asana)
                    }
                } catch (ex: Exception) {
                    // Set a placeholder image if all else fails
                    binding.ivAsanaImage.setImageResource(R.drawable.placeholder_asana)
                }
            }
            
            // Set click listener
            itemView.setOnClickListener {
                onAsanaClick(asana)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsanaViewHolder {
        val binding = ItemYogaAsanaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AsanaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AsanaViewHolder, position: Int) {
        android.util.Log.d("YogaAsanaAdapter", "Binding view holder at position $position for asana: ${asanas[position].name}")
        holder.bind(asanas[position])
    }

    override fun getItemCount(): Int = asanas.size

    fun updateAsanas(newAsanas: List<YogaAsana>) {
        asanas = newAsanas
        notifyDataSetChanged()
    }
}
