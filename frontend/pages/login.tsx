import styles from "@/styles/login.module.css";

export default function Login() {
    return (
        <div className={styles.container}>
            <div className={styles.content}>
                <p style={{textAlign: "center"}}>Login</p>
                <input type="email" name="email" placeholder="Email"/>
                <input type="password" name="password" placeholder="Password"/>
                <button type="button">Submit</button>
            </div>
        </div>
    )
}