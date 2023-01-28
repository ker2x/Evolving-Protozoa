package protoevo.utils;

import java.io.Serializable;

public class Vector2 implements Serializable
{
    public static final Vector2 ZERO = new Vector2(0, 0);
    private static final long serialVersionUID = 8642244552320036511L;
	private float x;
	private float y;
	
	public Vector2(float x, float y)
	{
		this.setX(x);
		this.setY(y);
	}

	public static Vector2 fromAngle(float angle) {
		return new Vector2((float) Math.cos(angle), (float) Math.sin(angle));
	}

	public float len2()
	{
		return x*x + y*y;
	}
	
	public float len(){
		return (float) Math.sqrt(x*x + y*y);
	}

	public Vector2 copy() {
		return new Vector2(x, y);
	}

	public Vector2 translate(Vector2 dv) {
		x += dv.getX();
		y += dv.getY();
		return this;
	}

	public Vector2 translate(float dx, float dy) {
		x += dx;
		y += dy;
		return this;
	}

	public Vector2 scale(float s) {
		x *= s;
		y *= s;
		return this;
	}
	
	public Vector2 add(Vector2 b){
		return new Vector2(getX() + b.getX(), getY() + b.getY());
	}
	
	public Vector2 sub(Vector2 b){
		return new Vector2(getX() - b.getX(), getY() - b.getY());
	}
	
	public Vector2 mul(float s) {
		return new Vector2(s*getX(), s*getY());
	}
	
	public float dot(Vector2 b) {
		return ( getX()*b.getX() + getY()*b.getY() );
	}
	
	public Vector2 perp() {
		return new Vector2(-getY(), getX());
	}
	
	public Vector2 rotate(float angle)
	{
		float c = (float) CachedMath.cos(angle);
		float s = (float) CachedMath.sin(angle);
		return new Vector2(x*c - y*s, x*s + y*c);
	}

	public Vector2 turn(float angle) {
		float c = (float) CachedMath.cos(angle);
		float s = (float) CachedMath.sin(angle);
		float xNew = x*c - y*s;
		float yNew = x*s + y*c;
		x = xNew;
		y = yNew;
		return this;
	}
	
	public Vector2 unit()
	{
		float len = len();
		if (len == 0)
			return new Vector2(0, 0);
		return new Vector2(x / len, y / len);
	}

	public Vector2 nor() {
		float len = len();
		if (len == 0)
			return this;
		x /= len;
		y /= len;
		return this;
	}
	
	public float angle() {
		return (float) Math.atan2(getY(), getX());
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public Vector2 setLength(float targetLen)
	{
		float currentLen = len();
		x *= targetLen / currentLen;
		y *= targetLen / currentLen;
		return this;
	}
	
	public Vector2 setDir(Vector2 other)
	{
		return other.setLength(len());
	}
	
	public String toString()
	{
		return "(" + x + ", " + y + ")";
	}

	public float angleBetween(Vector2 other)
	{
		float a = len2(), b = other.len2();
		return (float) Math.acos(dot(other) / Math.sqrt(a*b));
	}

	public float distanceTo(Vector2 other) {
		float dx = getX() - other.getX();
		float dy = getY() - other.getY();
		return (float) Math.sqrt(dx*dx + dy*dy);
	}

	public float squareDistanceTo(Vector2 other) {
		float dx = getX() - other.getX();
		float dy = getY() - other.getY();
		return dx*dx + dy*dy;
	}

	public boolean equals(Object o) {
		if (o instanceof Vector2) {
			Vector2 v = (Vector2) o;
			return v.getX() == getX() && v.getY() == getY();
		}
		return false;
	}

	public void moveAway(Vector2 other, float amount) {
		float dx = getX() - other.getX();
		float dy = getY() - other.getY();
		float l = (float) Math.sqrt(dx*dx + dy*dy);
		x = other.getX() + dx * amount / l;
		y = other.getY() + dy * amount / l;
	}

	public Vector2 set(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	public Vector2 set(Vector2 v) {
		this.x = v.getX();
		this.y = v.getY();
		return this;
	}

	public Vector2 take(Vector2 pos) {
		x -= pos.getX();
		y -= pos.getY();
		return this;
	}
}
