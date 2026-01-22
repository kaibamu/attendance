import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGen {
	public static void main(String[] args) {
		BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
		System.out.println("admin = " + enc.encode("admin"));
		System.out.println("employee = " + enc.encode("employee"));
	}
}
