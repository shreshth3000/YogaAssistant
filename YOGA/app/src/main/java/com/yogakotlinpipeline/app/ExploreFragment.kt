package com.yogakotlinpipeline.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.yogakotlinpipeline.app.databinding.FragmentExploreBinding

class ExploreFragment : Fragment() {
    
    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var poseAdapter: PoseAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("ExploreFragment", "=== onViewCreated called ===")
        setupRecyclerView()
        loadPoses()
        Log.d("ExploreFragment", "=== Setup complete ===")
    }
    
    private fun setupRecyclerView() {
        poseAdapter = PoseAdapter { pose ->
            // Navigate to YouTube video with pose information
            Log.d("ExploreFragment", "=== Pose clicked: ${pose.name} ===")
            Log.d("ExploreFragment", "Display name: ${pose.displayName}")
            Log.d("ExploreFragment", "Attempting navigation to youtubeVideoFragment...")
            
            val bundle = Bundle().apply {
                putString("pose_name", pose.name)
                putString("pose_display_name", pose.displayName)
                putString("pose_description", pose.description)
                putString("pose_difficulty", pose.difficulty)
            }
            
            Log.d("ExploreFragment", "Bundle created with pose_name: ${bundle.getString("pose_name")}")
            
            try {
                findNavController().navigate(R.id.action_exploreFragment_to_youtubeVideoFragment, bundle)
                Log.d("ExploreFragment", "Navigation successful!")
            } catch (e: Exception) {
                Log.e("ExploreFragment", "Navigation failed: ${e.message}", e)
                // Show error to user
                Toast.makeText(context, "Navigation failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        binding.recyclerViewPoses.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = poseAdapter
        }
        
        // Set up bottom navigation click listeners
        setupBottomNavigation()
    }
    
    private fun setupBottomNavigation() {
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_exploreFragment_to_inside1Fragment)
        }
        
        binding.btnFlows.setOnClickListener {
            findNavController().navigate(R.id.action_exploreFragment_to_inside3Fragment)
        }
        
        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_exploreFragment_to_inside4Fragment)
        }
        
        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_exploreFragment_to_profileFragment)
        }
    }
    
    private fun loadPoses() {
        val poses = listOf(
            Pose(
                name = "Dandasana",
                displayName = "Staff Pose",
                description = "A foundational seated pose that improves posture and strengthens the back muscles. Perfect for beginners to learn proper alignment.",
                difficulty = "Beginner"
            ),
            Pose(
                name = "Warrior ii pose",
                displayName = "Warrior II",
                description = "A powerful standing pose that builds strength and stamina. Improves balance and opens the hips and chest.",
                difficulty = "Intermediate"
            ),
            Pose(
                name = "Vrksasana",
                displayName = "Tree Pose",
                description = "A balancing pose that improves focus and concentration. Strengthens the legs and improves posture.",
                difficulty = "Intermediate"
            ),
            Pose(
                name = "Trikonsana",
                displayName = "Triangle Pose",
                description = "A standing pose that improves balance and stretches the sides of the body. Opens the hips and strengthens the legs.",
                difficulty = "Intermediate"
            ),
            Pose(
                name = "Parsvottanasana",
                displayName = "Pyramid Pose",
                description = "A forward bend that stretches the hamstrings and improves flexibility. Requires good balance and core strength.",
                difficulty = "Advanced"
            ),
            Pose(
                name = "Paschimottanasana",
                displayName = "Seated Forward Bend",
                description = "A calming pose that stretches the back body. Promotes relaxation and improves flexibility.",
                difficulty = "Intermediate"
            ),
            Pose(
                name = "Prasarita Padottanasana",
                displayName = "Wide-Legged Forward Bend",
                description = "A pose that stretches the inner thighs and hamstrings. Improves balance and opens the hips.",
                difficulty = "Beginner"
            ),
            Pose(
                name = "Boat pose",
                displayName = "Boat Pose",
                description = "A core-strengthening pose that builds balance and concentration. Improves core strength and posture.",
                difficulty = "Beginner"
            ),
            Pose(
                name = "Chakrasana",
                displayName = "Wheel Pose",
                description = "A backbend that opens the chest and shoulders. Requires significant back flexibility and strength.",
                difficulty = "Advanced"
            ),
            Pose(
                name = "Gomukhasana",
                displayName = "Cow Face Pose",
                description = "A seated pose that stretches the shoulders and hips. Improves posture and opens tight areas.",
                difficulty = "Intermediate"
            ),
            Pose(
                name = "King pigeon",
                displayName = "King Pigeon",
                description = "A backbend that opens the chest and shoulders. Requires exceptional back flexibility and strength.",
                difficulty = "Advanced"
            ),
            Pose(
                name = "Urdhva Prasarita eka padasana",
                displayName = "Standing Split",
                description = "A standing pose that improves balance and flexibility. Requires good hamstring flexibility.",
                difficulty = "Intermediate"
            ),
            Pose(
                name = "Yoganidrasana",
                displayName = "Yogic Sleep",
                description = "A relaxing pose for deep rest and meditation. Promotes relaxation and stress relief.",
                difficulty = "Advanced"
            )
        )
        
        poseAdapter.submitList(poses)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    data class Pose(
        val name: String,
        val displayName: String,
        val description: String,
        val difficulty: String
    )
}
