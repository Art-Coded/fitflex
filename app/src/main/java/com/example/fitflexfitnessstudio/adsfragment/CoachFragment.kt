import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.fitflexfitnessstudio.R

class CoachFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_coach, container, false)

        val tvCoachName = view.findViewById<TextView>(R.id.tvCoachName)
        val tvCoachSpecialty = view.findViewById<TextView>(R.id.tvCoachSpecialty)
        val tvCoachSchedule = view.findViewById<TextView>(R.id.tvCoachSchedule)
        val btnFacebook = view.findViewById<Button>(R.id.btnFacebook)

        // Retrieve arguments
        val name = arguments?.getString("name")
        val specialty = arguments?.getString("specialty")
        val schedule = arguments?.getString("schedule")
        val facebookUrl = arguments?.getString("facebookUrl")

        // Populate UI
        tvCoachName.text = name
        tvCoachSpecialty.text = "Specialty: $specialty"
        tvCoachSchedule.text = "Schedule: $schedule"

        // Handle Facebook button click
        btnFacebook.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl))
            startActivity(intent)
        }

        return view
    }
}